package com.laboschqpa.filehost.service;

import com.laboschqpa.filehost.entity.S3FileEntity;
import com.laboschqpa.filehost.enums.IndexedFileStatus;
import com.laboschqpa.filehost.enums.apierrordescriptor.UploadApiError;
import com.laboschqpa.filehost.exceptions.NotImplementedException;
import com.laboschqpa.filehost.exceptions.apierrordescriptor.QuotaExceededException;
import com.laboschqpa.filehost.exceptions.apierrordescriptor.StreamLengthLimitExceededException;
import com.laboschqpa.filehost.exceptions.apierrordescriptor.UploadException;
import com.laboschqpa.filehost.model.buffer.AutoResizingMemoryDuplexByteStore;
import com.laboschqpa.filehost.model.buffer.DuplexByteStore;
import com.laboschqpa.filehost.model.inputstream.RingBufferOverDuplexByteStoreInputStream;
import com.laboschqpa.filehost.repo.S3FileEntityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.tika.mime.MimeTypes;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

import java.io.IOException;
import java.io.InputStream;

@Log4j2
@RequiredArgsConstructor
@Service
public class DefaultS3FileSaver implements S3FileSaver {
    private static final int KB = 1000;
    private static final int MB = 1000 * KB;

    private static final int MULTIPART_CHUNK_SIZE = 5 * MB;
    private static final int RING_BUFFER_RING_SIZE = MULTIPART_CHUNK_SIZE * 2 + 2 * KB;
    private static final int RING_BUFFER_READ_BUFFER_SIZE = 8 * KB;

    private static final String DEFAULT_CACHE_CONTROL = "private, immutable, max-age=31536000";


    private final S3FileEntityRepository s3FileEntityRepository;
    private final S3Client s3Client;

    @Override
    public void writeFromStream(InputStream fileUploadingInputStream, S3FileEntity s3FileEntity) {
        try (DuplexByteStore duplexByteStore
                     = new AutoResizingMemoryDuplexByteStore(64 * KB, 1.5)
        ) {
            final RingBufferOverDuplexByteStoreInputStream ringBufferInputStream = new RingBufferOverDuplexByteStoreInputStream(
                    fileUploadingInputStream, duplexByteStore, RING_BUFFER_RING_SIZE, RING_BUFFER_READ_BUFFER_SIZE
            );

            final boolean wasEofReached = ringBufferInputStream.readUntilRingIsFullOrEndOfStreamReached();

            if (wasEofReached) {
                uploadWholeObject(s3FileEntity, ringBufferInputStream, ringBufferInputStream.getCurrentUnreadSize());
            } else {
                uploadMultipartObject(s3FileEntity, ringBufferInputStream);
            }

            s3FileEntity.setStatus(IndexedFileStatus.UPLOAD_STREAM_SAVED);
        } catch (QuotaExceededException | StreamLengthLimitExceededException e) {
            s3FileEntity.setStatus(IndexedFileStatus.ABORTED_BY_FILE_HOST);
            throw e;
        } catch (RuntimeException e) {
            s3FileEntity.setStatus(IndexedFileStatus.FAILED);
            throw e;
        } catch (IOException e) {
            s3FileEntity.setStatus(IndexedFileStatus.FAILED);
            throw new UploadException(UploadApiError.IO_EXCEPTION_WHILE_SAVING_STREAM, "Cannot write stream to file!", e);
        } finally {
            s3FileEntityRepository.save(s3FileEntity);
        }
    }

    /**
     * Non-multipart file upload.
     */
    void uploadWholeObject(S3FileEntity s3FileEntity, InputStream inputStream, long size) {
        log.trace("Uploading {} to S3 as whole object", s3FileEntity.getId());

        final PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(s3FileEntity.getBucket())
                .key(s3FileEntity.getObjectKey())
                .contentType(s3FileEntity.getMimeType())
                .cacheControl(DEFAULT_CACHE_CONTROL)
                .contentDisposition(getContentDispositionHeader(s3FileEntity))
                .build();


        final RequestBody requestBody = RequestBody.fromInputStream(inputStream, size);
        final PutObjectResponse putObjectResponse = s3Client.putObject(putObjectRequest, requestBody);

        log.info("Uploaded {} to S3 as whole object. Received eTag: {}", s3FileEntity.getId(), putObjectResponse.eTag());
    }

    void uploadMultipartObject(S3FileEntity s3FileEntity, RingBufferOverDuplexByteStoreInputStream ringBufferInputStream) throws IOException {
        //TODO: AWS PUT Object Parts
        log.trace("Uploading {} to S3 as multipart object", s3FileEntity.getId());

        boolean isEof = false;
        while (!isEof) {
            isEof = ringBufferInputStream.readUntilRingIsFullOrEndOfStreamReached();
            while (ringBufferInputStream.getCurrentUnreadSize() > 0 && !isEof) {
                if (ringBufferInputStream.read() == -1)
                    isEof = true;
            }
        }

        throw new NotImplementedException();
    }

    static String getContentDispositionHeader(S3FileEntity s3FileEntity) {
        if (MimeTypes.OCTET_STREAM.equals(s3FileEntity.getMimeType())) {
            return "attachment; filename=\"" + s3FileEntity.getOriginalFileName() + "\"";
        }
        return null;
    }
}
