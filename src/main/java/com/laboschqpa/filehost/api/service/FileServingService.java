package com.laboschqpa.filehost.api.service;

import com.laboschqpa.filehost.config.annotation.ExceptionWrappedFileServingClass;
import com.laboschqpa.filehost.config.filter.WrappedFileServingHttpServletRequest;
import com.laboschqpa.filehost.config.filter.WrappedFileServingRequestDto;
import com.laboschqpa.filehost.entity.IndexedFileEntity;
import com.laboschqpa.filehost.enums.IndexedFileStatus;
import com.laboschqpa.filehost.exceptions.fileserving.*;
import com.laboschqpa.filehost.model.file.IndexedFile;
import com.laboschqpa.filehost.model.file.StoredFile;
import com.laboschqpa.filehost.model.file.factory.UploadableFileFactory;
import com.laboschqpa.filehost.model.file.DownloadableFile;
import com.laboschqpa.filehost.model.file.factory.DownloadableFileFactory;
import com.laboschqpa.filehost.model.streamtracking.TrackingInputStream;
import com.laboschqpa.filehost.model.streamtracking.TrackingInputStreamFactory;
import com.laboschqpa.filehost.repo.IndexedFileEntityRepository;
import com.laboschqpa.filehost.util.StoredFileUtils;
import lombok.RequiredArgsConstructor;
import org.apache.tomcat.util.http.fileupload.FileItemIterator;
import org.apache.tomcat.util.http.fileupload.FileItemStream;
import org.apache.tomcat.util.http.fileupload.MultipartStream;
import org.apache.tomcat.util.http.fileupload.servlet.ServletFileUpload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.*;
import java.nio.file.Path;

@RequiredArgsConstructor
@Service
@ExceptionWrappedFileServingClass
public class FileServingService {
    private static final Logger logger = LoggerFactory.getLogger(FileServingService.class);
    private static final Integer UPLOADED_FILE_NAME_MAX_LENGTH = 200;

    private final ServletFileUpload servletFileUpload = new ServletFileUpload();

    @Value("${filehost.storedfiles.upload.filemaxsize}")
    private Long uploadFileMaxSize;

    private final DownloadableFileFactory downloadableFileFactory;
    private final UploadableFileFactory uploadableFileFactory;
    private final IndexedFileEntityRepository indexedFileEntityRepository;
    private final TrackingInputStreamFactory trackingInputStreamFactory;
    private final StoredFileUtils storedFileUtils;

    public ResponseEntity<Resource> downloadFile(WrappedFileServingHttpServletRequest request) {
        DownloadableFile downloadableFile = downloadableFileFactory.from(request.getWrappedFileServingRequestDto());

        if (downloadableFile.isAvailable()) {
            String ifNoneMatchHeaderValue = request.getHeader(HttpHeaders.IF_NONE_MATCH);
            if (ifNoneMatchHeaderValue != null
                    && !ifNoneMatchHeaderValue.isBlank()
                    && ifNoneMatchHeaderValue.equals(downloadableFile.getETag())) {
                logger.trace("ETag matches. Returning 304 - Not modified");
                return new ResponseEntity<>(HttpStatus.NOT_MODIFIED);
            }

            HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            httpHeaders.setContentLength(downloadableFile.getSize());
            httpHeaders.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + downloadableFile.getOriginalFileName() + "\"");

            TrackingInputStream downloadTrackingStream = trackingInputStreamFactory.createForFileDownload(downloadableFile.getStream());
            return new ResponseEntity<>(new InputStreamResource(downloadTrackingStream), httpHeaders, HttpStatus.OK);
        } else {
            throw new FileIsNotAvailableException("The requested file is not available for download. File status: " + downloadableFile.getStatus());
        }
    }

    /**
     * Allows uploading exactly one file in a multipart request.
     * <br>
     * File uploads should be parallelized in multiple HTTP requests.
     *
     * @return {@code indexedFileEntity} of the newly uploaded file
     */
    public IndexedFileEntity uploadFile(WrappedFileServingHttpServletRequest request) {
        if (!StringUtils.startsWithIgnoreCase(request.getContentType(), "multipart/")) {
            throw new InvalidUploadRequestException("The request is not a multipart request.");
        }
        InputStream uploadedFileInputStream = null;
        try {
            FileItemIterator iterator = servletFileUpload.getItemIterator(request);
            if (!iterator.hasNext()) {
                throw new InvalidUploadRequestException("No fields present in the multipart request!");
            }

            FileItemStream uploadedFile = iterator.next();
            String fieldName = uploadedFile.getFieldName();
            if (uploadedFile.isFormField()) {
                throw new InvalidUploadRequestException("Unexpected multipart form field is present in HTTP body: " + fieldName);
            }

            String uploadedFileName = uploadedFile.getName().trim();
            if (uploadedFileName.length() > UPLOADED_FILE_NAME_MAX_LENGTH) {
                uploadedFileName = uploadedFileName.substring(uploadedFileName.length() - UPLOADED_FILE_NAME_MAX_LENGTH);
            }

            uploadedFileInputStream = uploadedFile.openStream();
            logger.debug("Multipart file field {} with fileName {} detected.", fieldName, uploadedFileName);

            IndexedFileEntity newlySavedFile = saveNewFile(request.getWrappedFileServingRequestDto(), uploadedFileInputStream,
                    uploadedFileName, null);//TODO: Get the initial file size approximation from a form field
            handleStreamClose(uploadedFileInputStream, false);
            return newlySavedFile;
        } catch (InvalidUploadRequestException | QuotaExceededException e) {
            handleStreamClose(uploadedFileInputStream, false);
            throw e;
        } catch (Exception e) {
            handleStreamClose(uploadedFileInputStream, true);
            throw new FileSavingException("Exception occurred while saving file.", e);
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
                logger.warn("Couldn't close upload stream!", e);
            }
        }
    }

    private IndexedFileEntity saveNewFile(WrappedFileServingRequestDto wrappedFileServingRequestDto,
                                          InputStream fileUploadingInputStream, String uploadedFileName, Long approximateFileSize) {
        StoredFile newUploadableFile = null;
        try {
            newUploadableFile = uploadableFileFactory.fromFileUploadRequest(wrappedFileServingRequestDto, uploadedFileName);

            TrackingInputStream trackingInputStream = trackingInputStreamFactory.createForFileUpload(fileUploadingInputStream);
            trackingInputStream.setLimit(uploadFileMaxSize);

            newUploadableFile.saveFromStream(trackingInputStream, approximateFileSize);
            logger.debug("New file uploaded and saved: {}", newUploadableFile.getIndexedFileEntity().toString());
            return newUploadableFile.getIndexedFileEntity();

        } catch (StreamLengthLimitExceededException | QuotaExceededException e) {
            logger.info("File upload was aborted! indexedFileId: {}", getFileIdToPrintOnFailure(newUploadableFile), e);

            if (newUploadableFile != null) {
                markFileAs(IndexedFileStatus.ABORTED_BY_FILE_HOST, newUploadableFile);
                cleanUpFile(IndexedFileStatus.CLEANED_UP_AFTER_ABORTED, newUploadableFile);
            }
            throw e;
        } catch (Exception e) {
            logger.info("File upload failed! indexedFileId: {}", getFileIdToPrintOnFailure(newUploadableFile), e);

            if (newUploadableFile != null) {
                markFileAs(IndexedFileStatus.FAILED, newUploadableFile);
                cleanUpFile(IndexedFileStatus.CLEANED_UP_AFTER_FAILED, newUploadableFile);
            }
            throw new FileSavingException("Exception while handling saving of the uploaded file!", e);
        }
    }

    private static String getFileIdToPrintOnFailure(IndexedFile indexedFile) {
        if (indexedFile != null && indexedFile.getIndexedFileEntity() != null)
            return String.valueOf(indexedFile.getIndexedFileEntity().getId());
        else
            return "null";
    }

    private void markFileAs(IndexedFileStatus statusToMarkAs, IndexedFile indexedFile) {
        try {
            indexedFile.getIndexedFileEntity().setStatus(statusToMarkAs);
            indexedFileEntityRepository.save(indexedFile.getIndexedFileEntity());
        } catch (Exception ex) {
            logger.error("Exception while marking uploaded file as {}", statusToMarkAs, ex);
        }
    }

    private void cleanUpFile(IndexedFileStatus statusAfterCleanup, StoredFile storedFile) {
        try {
            if (storedFile != null) {
                String fullPath = storedFileUtils.getFullPathFromStoredFileEntityPath(storedFile.getIndexedFileEntity().getPath());
                java.nio.file.Files.deleteIfExists(Path.of(fullPath));

                storedFile.getIndexedFileEntity().setStatus(statusAfterCleanup);
                indexedFileEntityRepository.save(storedFile.getIndexedFileEntity());
                logger.trace("File {} cleaned up: {}", storedFile.getIndexedFileEntity().getId(), statusAfterCleanup);
            }
        } catch (Exception ex) {
            logger.error("Couldn't clean up file after QuotaExceededException!", ex);
        }
    }

}
