package com.laboschqpa.filehost.service;

import com.laboschqpa.filehost.entity.StoredFileEntity;
import com.laboschqpa.filehost.enums.IndexedFileStatus;
import com.laboschqpa.filehost.enums.apierrordescriptor.UploadApiError;
import com.laboschqpa.filehost.exceptions.apierrordescriptor.UploadException;
import com.laboschqpa.filehost.exceptions.apierrordescriptor.QuotaExceededException;
import com.laboschqpa.filehost.model.streamtracking.TrackingInputStream;
import com.laboschqpa.filehost.repo.StoredFileEntityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;

@RequiredArgsConstructor
@Service
public class QuotaAllocatingStoredFileSaver implements StoredFileSaver {
    private static final int EOF = -1;

    private final StoredFileEntityRepository storedFileEntityRepository;
    private final StoredFileQuotaAllocator storedFileQuotaAllocator;

    @Value("${filehost.storedfiles.upload.filesavingmaxbuffersize}")
    private Integer fileSavingMaxBufferSize;

    @Override
    public void writeFromStream(TrackingInputStream streamToWriteIntoFile, File targetFile, StoredFileEntity storedFileEntity, Long approximateFileSize) {
        handleDirectoryStructureBeforeWritingToFile(targetFile);

        try (BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(
                new FileOutputStream(targetFile, false), fileSavingMaxBufferSize)
        ) {
            copyWholeStreamWhileAllocating(streamToWriteIntoFile, bufferedOutputStream, storedFileEntity, approximateFileSize);
            bufferedOutputStream.flush();
            storedFileEntity.setStatus(IndexedFileStatus.UPLOADED);
        } catch (QuotaExceededException e) {
            storedFileEntity.setStatus(IndexedFileStatus.ABORTED_BY_FILE_HOST);
            throw e;
        } catch (Exception e) {
            storedFileEntity.setStatus(IndexedFileStatus.FAILED);
            throw new UploadException(UploadApiError.CANNOT_WRITE_STREAM_TO_FILE, "Cannot write stream to file!", e);
        } finally {
            storedFileEntity.setSize(streamToWriteIntoFile.getCountOfReadBytes());
            storedFileEntityRepository.save(storedFileEntity);
        }
    }

    private void copyWholeStreamWhileAllocating(TrackingInputStream inputStream, OutputStream outputStream, StoredFileEntity storedFileEntity, Long approximateFileSize) throws IOException {
        final byte[] copyBuffer = new byte[fileSavingMaxBufferSize];
        boolean endOfStream = false;
        while (!endOfStream) {
            final long maxBytesToCopyInCurrentIteration = storedFileQuotaAllocator.allocateQuota(storedFileEntity, inputStream, approximateFileSize);
            endOfStream = endCheckingCopyNBytes(inputStream, outputStream, copyBuffer, maxBytesToCopyInCurrentIteration);
        }
    }

    /**
     * The {@code bytesToCopy} should be correct in most of the cases to copy exactly the whole uploaded file,
     * so this function tries to copy {@code bytesToCopy} bytes and then one more to check if end of stream is reached.
     * <br>
     * The copying itself is very similar to what {@link org.apache.commons.io.IOUtils} does.
     *
     * @return {@code True} if end of stream is reached, {@code false} otherwise.
     */
    private boolean endCheckingCopyNBytes(final InputStream inputStream, final OutputStream outputStream,
                                          final byte[] buffer, long bytesToCopy) throws IOException {
        final int bufferLength = buffer.length;
        while (bytesToCopy > 0) {

            int bytesToRead;
            if (bufferLength < bytesToCopy) {
                bytesToRead = bufferLength;
            } else {
                bytesToRead = (int) bytesToCopy;
            }

            int readBytes = inputStream.read(buffer, 0, bytesToRead);
            if (readBytes == EOF) {
                return true;
            } else {
                outputStream.write(buffer, 0, readBytes);
                bytesToCopy -= readBytes;
            }
        }

        //End checking
        final int endCheckingByte = inputStream.read();
        if (endCheckingByte == EOF) {
            return true;
        } else {
            outputStream.write(endCheckingByte);
            return false;
        }
    }

    private void handleDirectoryStructureBeforeWritingToFile(File file) {
        if (!file.getParentFile().exists()) {
            if (!file.getParentFile().mkdirs()) {
                throw new UploadException(UploadApiError.ERROR_DURING_SAVING_FILE,
                        "Couldn't create containing directory: " + file.getParentFile());
            }
        }
    }
}
