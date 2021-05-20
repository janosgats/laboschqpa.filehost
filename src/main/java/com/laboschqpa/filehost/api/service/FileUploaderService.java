

package com.laboschqpa.filehost.api.service;

import com.laboschqpa.filehost.annotation.ExceptionWrappedFileServingClass;
import com.laboschqpa.filehost.entity.IndexedFileEntity;
import com.laboschqpa.filehost.enums.IndexedFileStatus;
import com.laboschqpa.filehost.enums.UploadType;
import com.laboschqpa.filehost.enums.apierrordescriptor.UploadApiError;
import com.laboschqpa.filehost.exceptions.apierrordescriptor.InvalidUploadRequestException;
import com.laboschqpa.filehost.exceptions.apierrordescriptor.QuotaExceededException;
import com.laboschqpa.filehost.exceptions.apierrordescriptor.StreamLengthLimitExceededException;
import com.laboschqpa.filehost.exceptions.apierrordescriptor.UploadException;
import com.laboschqpa.filehost.model.file.IndexedFile;
import com.laboschqpa.filehost.model.file.UploadableFile;
import com.laboschqpa.filehost.model.file.factory.UploadableFileFactory;
import com.laboschqpa.filehost.model.inputstream.*;
import com.laboschqpa.filehost.model.streamtracking.QuotaAllocatingInputStreamFactory;
import com.laboschqpa.filehost.model.streamtracking.TrackingInputStreamFactory;
import com.laboschqpa.filehost.model.upload.FileUploadRequest;
import com.laboschqpa.filehost.repo.IndexedFileEntityRepository;
import com.laboschqpa.filehost.service.imagevariant.VariantCreatorService;
import com.laboschqpa.filehost.util.FileUploadUtils;
import com.laboschqpa.filehost.util.IOExceptionUtils;
import com.laboschqpa.filehost.util.fileuploadconfigurer.FileUploadConfigurerFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.tika.detect.Detector;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;
import org.apache.tomcat.util.http.fileupload.FileItemIterator;
import org.apache.tomcat.util.http.fileupload.FileItemStream;
import org.apache.tomcat.util.http.fileupload.MultipartStream;
import org.apache.tomcat.util.http.fileupload.servlet.ServletFileUpload;
import org.apache.tomcat.util.http.fileupload.util.Streams;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.io.InputStream;
import java.io.SequenceInputStream;

@Log4j2
@RequiredArgsConstructor
@Service
@ExceptionWrappedFileServingClass
public class FileUploaderService {
    private static final int MB = 1000 * 1000;
    private static final int TIKA_REREADABLE_STREAM_MAX_BYTES_IN_MEMORY = 1 * MB;
    private static final String FORM_FIELD_NAME_APPROXIMATE_FILE_SIZE = "approximateFileSize";
    private static final String MIME_TYPE_IMAGE = "image";

    private final ServletFileUpload servletFileUpload = new ServletFileUpload();

    private final FileUploadConfigurerFactory fileUploadConfigurerFactory;
    private final VariantCreatorService variantCreatorService;
    private final UploadableFileFactory uploadableFileFactory;
    private final IndexedFileEntityRepository indexedFileEntityRepository;
    private final TrackingInputStreamFactory trackingInputStreamFactory;
    private final QuotaAllocatingInputStreamFactory quotaAllocatingInputStreamFactory;
    private final Detector tikaDetector;

    /**
     * Allows uploading exactly one file in a multipart request.
     * <br>
     * File uploads should be parallelized in multiple HTTP requests.
     *
     * @return {@code indexedFileEntity} of the newly uploaded file
     */
    public IndexedFileEntity uploadFile(FileUploadRequest fileUploadRequest, HttpServletRequest httpServletRequest) {
        if (!StringUtils.startsWithIgnoreCase(httpServletRequest.getContentType(), "multipart/")) {
            throw new InvalidUploadRequestException("The request is not a multipart request.");
        }
        TrackingInputStream uploadedFileTrackingInputStream = null;
        try {
            FileItemIterator iterator = servletFileUpload.getItemIterator(httpServletRequest);
            if (!iterator.hasNext()) {
                throw new InvalidUploadRequestException("No fields present in the multipart request!");
            }

            Long approximateFileSize = null;

            FileItemStream uploadedFile;

            FileItemStream firstItem = iterator.next();
            if (firstItem.isFormField() && FORM_FIELD_NAME_APPROXIMATE_FILE_SIZE.equals(firstItem.getFieldName())) {
                approximateFileSize = Long.parseLong(Streams.asString(firstItem.openStream()));
                uploadedFile = iterator.next();
            } else {
                uploadedFile = firstItem;
            }

            String fieldName = uploadedFile.getFieldName();
            if (uploadedFile.isFormField()) {
                throw new InvalidUploadRequestException("Unexpected multipart form field is present in HTTP body: " + fieldName);
            }

            final String normalizedFileName = FileUploadUtils.createNormalizedFileName(uploadedFile);

            uploadedFileTrackingInputStream = trackingInputStreamFactory.createForFileUpload(uploadedFile.openStream());
            fileUploadConfigurerFactory.get(fileUploadRequest.getForcedFileType()).applyMaxFileSize(uploadedFileTrackingInputStream);
            log.debug("Multipart file field {} with fileName {} detected.", fieldName, normalizedFileName);

            IndexedFileEntity newlySavedFile = saveNewFile(fileUploadRequest, uploadedFileTrackingInputStream,
                    normalizedFileName, approximateFileSize);

            handleStreamClose(uploadedFileTrackingInputStream, false);

            if (fileUploadRequest.getUploadType() != UploadType.IMAGE_VARIANT) {
                createFileVariantsIfNeeded(newlySavedFile);
            }

            return newlySavedFile;
        } catch (InvalidUploadRequestException | QuotaExceededException e) {
            handleStreamClose(uploadedFileTrackingInputStream, false);
            throw e;
        } catch (Exception e) {
            handleStreamClose(uploadedFileTrackingInputStream, true);

            if (e instanceof StreamLengthLimitExceededException) {
                throw (StreamLengthLimitExceededException) e;
            }
            if (e instanceof UploadException) {
                throw (UploadException) e;
            }
            throw new UploadException(UploadApiError.ERROR_DURING_SAVING_FILE, "Exception occurred while saving file.", e);
        }
    }

    private void handleStreamClose(InputStream uploadedFileInputStream, boolean allowHardClose) {
        if (uploadedFileInputStream != null) {
            try {
                if (allowHardClose && uploadedFileInputStream instanceof MultipartStream.ItemInputStream) {
                /* We use "MultipartStream.ItemInputStream.close(true)", because the original close() doesn't close the HTTP stream
                   just skips the incoming bytes to the next multipart file.
                   We don't want to wait the end of the whole HTTP stream, so hard-close the stream by "close(true)".
                   This way we don't harm the resources unlike the original skipping & waiting behavior. */
                    ((MultipartStream.ItemInputStream) uploadedFileInputStream).close(true);
                } else {
                    uploadedFileInputStream.close();
                }
            } catch (Exception e) {
                log.warn("Couldn't close upload stream!", e);
            }
        }
    }

    private IndexedFileEntity saveNewFile(FileUploadRequest fileUploadRequest,
                                          InputStream fileUploadingInputStream, String uploadedFileName, Long approximateFileSize) {
        UploadableFile newUploadableFile = null;
        try {
            newUploadableFile = uploadableFileFactory.fromFileUploadRequest(fileUploadRequest, uploadedFileName);

            final QuotaAllocatingInputStream quotaAllocatingInputStream
                    = quotaAllocatingInputStreamFactory.from(fileUploadingInputStream, newUploadableFile.getEntity(), approximateFileSize);

            detectMimeTypeAndPersist(fileUploadRequest, quotaAllocatingInputStream, newUploadableFile);

            log.debug("New file uploaded and saved: {}", newUploadableFile.getEntity().toString());
            return newUploadableFile.getEntity();
        } catch (StreamLengthLimitExceededException | QuotaExceededException e) {
            log.info("File upload was aborted! indexedFileId: {}", getFileIdToPrintOnFailure(newUploadableFile), e);

            if (newUploadableFile != null) {
                markFileAs(IndexedFileStatus.ABORTED_BY_FILE_HOST, newUploadableFile);
                cleanUpFile(IndexedFileStatus.CLEANED_UP_AFTER_ABORTED, newUploadableFile);
            }
            throw e;
        } catch (Exception e) {
            log.info("File upload failed! indexedFileId: {}", getFileIdToPrintOnFailure(newUploadableFile), e);

            if (newUploadableFile != null) {
                markFileAs(IndexedFileStatus.FAILED, newUploadableFile);
                cleanUpFile(IndexedFileStatus.CLEANED_UP_AFTER_FAILED, newUploadableFile);
            }
            if (e instanceof UploadException) {
                throw e;
            }
            throw new UploadException(UploadApiError.ERROR_DURING_SAVING_FILE, "Exception while handling saving of the uploaded file!", e);
        }
    }

    void detectMimeTypeAndPersist(FileUploadRequest fileUploadRequest, CountingInputStreamInterface fileUploadingStream, UploadableFile uploadableFile) {
        final IndexedFileEntity fileEntity = uploadableFile.getEntity();

        fileEntity.setStatus(IndexedFileStatus.PRE_UPLOAD_PROCESSING);
        indexedFileEntityRepository.save(fileEntity);

        final MemoryReleasingRereadableInputStream rereadableInputStream
                = new MemoryReleasingRereadableInputStream(fileUploadingStream.getInputStream(), TIKA_REREADABLE_STREAM_MAX_BYTES_IN_MEMORY,
                false, false);
        try {
            log.trace("Bytes read before MIME type detection: {}", fileUploadingStream.getCountOfReadBytes());
            detectMimeTypeByTika(new NonCloseableInputStream(rereadableInputStream), fileEntity);
            log.trace("Bytes read after MIME type detection: {}", fileUploadingStream.getCountOfReadBytes());
            fileUploadConfigurerFactory.get(fileUploadRequest.getForcedFileType()).assertMimeType(uploadableFile);

            IOExceptionUtils.wrap(rereadableInputStream::rewind, "Cannot rewind rereadableInputStream after Tika MIME type detection.");

            SequenceInputStream sequenceInputStream = new SequenceInputStream(rereadableInputStream, fileUploadingStream.getInputStream());

            fileEntity.setStatus(IndexedFileStatus.UPLOADING);
            indexedFileEntityRepository.save(fileEntity);
            try {
                uploadableFile.saveFromStream(sequenceInputStream);
            } finally {
                fileEntity.setSize(fileUploadingStream.getCountOfReadBytes());
                indexedFileEntityRepository.save(fileEntity);
            }

            fileEntity.setStatus(IndexedFileStatus.AVAILABLE);
            indexedFileEntityRepository.save(fileEntity);
        } finally {
            //Only closing this stream to free the memory buffer
            IOExceptionUtils.swallowAndLog(rereadableInputStream::close,
                    "Error while closing RereadableInputStream to clean up the temporary file");
        }

    }

    private void detectMimeTypeByTika(InputStream inputStream, IndexedFileEntity indexedFileEntity) {
        try (TikaInputStream tikaInputStream = TikaInputStream.get(inputStream)) {
            Metadata metadata = new Metadata();
            metadata.add(Metadata.RESOURCE_NAME_KEY, indexedFileEntity.getName());
            MediaType detectedMediaType = tikaDetector.detect(tikaInputStream, metadata);

            log.trace("Detected MIME type: {}", detectedMediaType.toString());

            indexedFileEntity.setMimeType(detectedMediaType.getType() + "/" + detectedMediaType.getSubtype());
            indexedFileEntity.setIsImage(MIME_TYPE_IMAGE.equals(detectedMediaType.getType()));

            indexedFileEntityRepository.save(indexedFileEntity);
        } catch (Exception e) {
            log.error("Exception during Apache Tika mime type detection!", e);
        }
    }

    private static String getFileIdToPrintOnFailure(IndexedFile indexedFile) {
        if (indexedFile != null && indexedFile.getEntity() != null)
            return String.valueOf(indexedFile.getEntity().getId());
        else
            return "null";
    }

    private void cleanUpFile(IndexedFileStatus statusAfterSuccessfulCleanup, UploadableFile uploadableFile) {
        try {
            uploadableFile.cleanUpFailedUpload();
            markFileAs(statusAfterSuccessfulCleanup, uploadableFile);
            log.trace("File {} cleaned up: {}", uploadableFile.getEntity().getId(), statusAfterSuccessfulCleanup);
        } catch (Exception ex) {
            log.error("Couldn't clean up file after failed upload!", ex);
        }
    }

    private void markFileAs(IndexedFileStatus statusToMarkAs, IndexedFile indexedFile) {
        try {
            indexedFile.getEntity().setStatus(statusToMarkAs);
            indexedFileEntityRepository.save(indexedFile.getEntity());
        } catch (Exception ex) {
            log.error("Exception while marking uploaded file as {}", statusToMarkAs, ex);
        }
    }

    private void createFileVariantsIfNeeded(IndexedFileEntity newlySavedFile) {
        try {
            variantCreatorService.createMissingVariantsForFile(newlySavedFile);
        } catch (Exception e) {
            log.error("Error while creating variants for newly uploaded file. fileId: {}",
                    newlySavedFile.getId(), e);
        }
    }
}
