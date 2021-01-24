

package com.laboschqpa.filehost.api.service;

import com.laboschqpa.filehost.annotation.ExceptionWrappedFileServingClass;
import com.laboschqpa.filehost.api.dto.FileUploadRequest;
import com.laboschqpa.filehost.entity.IndexedFileEntity;
import com.laboschqpa.filehost.enums.IndexedFileStatus;
import com.laboschqpa.filehost.enums.apierrordescriptor.UploadApiError;
import com.laboschqpa.filehost.exceptions.apierrordescriptor.InvalidUploadRequestException;
import com.laboschqpa.filehost.exceptions.apierrordescriptor.QuotaExceededException;
import com.laboschqpa.filehost.exceptions.apierrordescriptor.StreamLengthLimitExceededException;
import com.laboschqpa.filehost.exceptions.apierrordescriptor.UploadException;
import com.laboschqpa.filehost.model.file.IndexedFile;
import com.laboschqpa.filehost.model.file.LocalDiskFile;
import com.laboschqpa.filehost.model.file.factory.UploadableFileFactory;
import com.laboschqpa.filehost.model.inputstream.QuotaAllocatingInputStream;
import com.laboschqpa.filehost.model.inputstream.TrackingInputStream;
import com.laboschqpa.filehost.model.streamtracking.TrackingInputStreamFactory;
import com.laboschqpa.filehost.repo.IndexedFileEntityRepository;
import com.laboschqpa.filehost.service.IndexedFileQuotaAllocator;
import com.laboschqpa.filehost.util.FileUploadUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.tomcat.util.http.fileupload.FileItemIterator;
import org.apache.tomcat.util.http.fileupload.FileItemStream;
import org.apache.tomcat.util.http.fileupload.MultipartStream;
import org.apache.tomcat.util.http.fileupload.servlet.ServletFileUpload;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.io.InputStream;

@Log4j2
@RequiredArgsConstructor
@Service
@ExceptionWrappedFileServingClass
public class FileUploaderService {
    private final ServletFileUpload servletFileUpload = new ServletFileUpload();

    @Value("${filehost.storedfiles.upload.filemaxsize}")
    private Long uploadFileMaxSize;

    private final UploadableFileFactory uploadableFileFactory;
    private final IndexedFileEntityRepository indexedFileEntityRepository;
    private final TrackingInputStreamFactory trackingInputStreamFactory;
    private final IndexedFileQuotaAllocator indexedFileQuotaAllocator;

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

            FileItemStream uploadedFile = iterator.next();
            String fieldName = uploadedFile.getFieldName();
            if (uploadedFile.isFormField()) {
                throw new InvalidUploadRequestException("Unexpected multipart form field is present in HTTP body: " + fieldName);
            }

            final String normalizedFileName = FileUploadUtils.createNormalizedFileName(uploadedFile);

            uploadedFileTrackingInputStream = trackingInputStreamFactory.createForFileUpload(uploadedFile.openStream());
            uploadedFileTrackingInputStream.setLimit(uploadFileMaxSize);
            log.debug("Multipart file field {} with fileName {} detected.", fieldName, normalizedFileName);

            IndexedFileEntity newlySavedFile = saveNewFile(fileUploadRequest, uploadedFileTrackingInputStream,
                    normalizedFileName, null);//TODO: Get the initial file size approximation from a form field
            handleStreamClose(uploadedFileTrackingInputStream, false);
            return newlySavedFile;
        } catch (InvalidUploadRequestException | QuotaExceededException e) {
            handleStreamClose(uploadedFileTrackingInputStream, false);
            throw e;
        } catch (Exception e) {
            handleStreamClose(uploadedFileTrackingInputStream, true);
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
        LocalDiskFile newUploadableFile = null;
        try {
            newUploadableFile = uploadableFileFactory.fromFileUploadRequest(fileUploadRequest, uploadedFileName);

            final QuotaAllocatingInputStream quotaAllocatingInputStream
                    = new QuotaAllocatingInputStream(fileUploadingInputStream, newUploadableFile.getIndexedFileEntity(),
                    indexedFileQuotaAllocator, approximateFileSize);

            newUploadableFile.saveFromStream(quotaAllocatingInputStream);
            log.debug("New file uploaded and saved: {}", newUploadableFile.getIndexedFileEntity().toString());
            return newUploadableFile.getIndexedFileEntity();

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
            throw new UploadException(UploadApiError.ERROR_DURING_SAVING_FILE, "Exception while handling saving of the uploaded file!", e);
        }
    }

    private static String getFileIdToPrintOnFailure(IndexedFile indexedFile) {
        if (indexedFile != null && indexedFile.getIndexedFileEntity() != null)
            return String.valueOf(indexedFile.getIndexedFileEntity().getId());
        else
            return "null";
    }

    private void cleanUpFile(IndexedFileStatus statusAfterSuccessfulCleanup, LocalDiskFile localDiskFile) {
        try {
            localDiskFile.cleanUpFailedUpload();
            markFileAs(statusAfterSuccessfulCleanup, localDiskFile);
            log.trace("File {} cleaned up: {}", localDiskFile.getIndexedFileEntity().getId(), statusAfterSuccessfulCleanup);
        } catch (Exception ex) {
            log.error("Couldn't clean up file after failed upload!", ex);
        }
    }

    private void markFileAs(IndexedFileStatus statusToMarkAs, IndexedFile indexedFile) {
        try {
            indexedFile.getIndexedFileEntity().setStatus(statusToMarkAs);
            indexedFileEntityRepository.save(indexedFile.getIndexedFileEntity());
        } catch (Exception ex) {
            log.error("Exception while marking uploaded file as {}", statusToMarkAs, ex);
        }
    }
}
