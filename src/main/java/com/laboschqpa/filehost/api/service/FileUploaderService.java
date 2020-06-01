

package com.laboschqpa.filehost.api.service;

import com.laboschqpa.filehost.config.annotation.ExceptionWrappedFileServingClass;
import com.laboschqpa.filehost.config.filter.AuthWrappedHttpServletRequest;
import com.laboschqpa.filehost.config.filter.WrappedExternalFileServingRequestDto;
import com.laboschqpa.filehost.entity.IndexedFileEntity;
import com.laboschqpa.filehost.enums.IndexedFileStatus;
import com.laboschqpa.filehost.exceptions.fileserving.*;
import com.laboschqpa.filehost.model.file.IndexedFile;
import com.laboschqpa.filehost.model.file.StoredFile;
import com.laboschqpa.filehost.model.file.factory.UploadableFileFactory;
import com.laboschqpa.filehost.model.streamtracking.TrackingInputStream;
import com.laboschqpa.filehost.model.streamtracking.TrackingInputStreamFactory;
import com.laboschqpa.filehost.repo.IndexedFileEntityRepository;
import com.laboschqpa.filehost.util.StoredFileUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.tomcat.util.http.fileupload.FileItemIterator;
import org.apache.tomcat.util.http.fileupload.FileItemStream;
import org.apache.tomcat.util.http.fileupload.MultipartStream;
import org.apache.tomcat.util.http.fileupload.servlet.ServletFileUpload;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.*;
import java.nio.file.Path;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

@Log4j2
@RequiredArgsConstructor
@Service
@ExceptionWrappedFileServingClass
public class FileUploaderService {
    private static final Integer UPLOADED_FILE_NAME_MAX_LENGTH = 200;

    private final DateTimeFormatter unnamedFileNameDatetimeFormatter = DateTimeFormatter.ofPattern("YYYY-MM-dd_HH-mm-ss").withZone(ZoneId.of("UTC"));
    private final ServletFileUpload servletFileUpload = new ServletFileUpload();

    @Value("${filehost.storedfiles.upload.filemaxsize}")
    private Long uploadFileMaxSize;

    private final UploadableFileFactory uploadableFileFactory;
    private final IndexedFileEntityRepository indexedFileEntityRepository;
    private final TrackingInputStreamFactory trackingInputStreamFactory;
    private final StoredFileUtils storedFileUtils;

    /**
     * Allows uploading exactly one file in a multipart request.
     * <br>
     * File uploads should be parallelized in multiple HTTP requests.
     *
     * @return {@code indexedFileEntity} of the newly uploaded file
     */
    public IndexedFileEntity uploadFile(AuthWrappedHttpServletRequest request) {
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

            final String normalizedFileName = normalizeUploadedFileName(uploadedFile);

            uploadedFileInputStream = uploadedFile.openStream();
            log.debug("Multipart file field {} with fileName {} detected.", fieldName, normalizedFileName);

            IndexedFileEntity newlySavedFile = saveNewFile(request.getWrappedExternalFileServingRequestDto(), uploadedFileInputStream,
                    normalizedFileName, null);//TODO: Get the initial file size approximation from a form field
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

    private String normalizeUploadedFileName(FileItemStream uploadedFile) {
        final String originalFileName = uploadedFile.getName();

        if (originalFileName == null || originalFileName.isBlank()) {
            return generateNameForUnnamedFile();
        } else {
            String normalizedFileName = originalFileName;

            if (normalizedFileName.contains("/")) {
                final String[] split = normalizedFileName.split("/");
                String buff = split[split.length - 1];
                if (buff.isBlank()) {
                    normalizedFileName = normalizedFileName.replace("/", "");
                } else {
                    normalizedFileName = buff;
                }
            }
            if (normalizedFileName.contains("\\")) {
                final String[] split = normalizedFileName.split("\\\\");
                String buff = split[split.length - 1];
                if (buff.isBlank()) {
                    normalizedFileName = normalizedFileName.replace("\\", "");
                } else {
                    normalizedFileName = buff;
                }
            }

            normalizedFileName = normalizedFileName.trim();

            if (normalizedFileName.length() > UPLOADED_FILE_NAME_MAX_LENGTH) {
                normalizedFileName = normalizedFileName.substring(normalizedFileName.length() - UPLOADED_FILE_NAME_MAX_LENGTH);
            }

            if (normalizedFileName.isBlank()) {
                return generateNameForUnnamedFile();
            }
            return normalizedFileName;
        }
    }

    private String generateNameForUnnamedFile() {
        final ZonedDateTime zonedDateTime = ZonedDateTime.now(ZoneId.of("UTC"));
        return String.format("unnamed_%s.file", zonedDateTime.format(unnamedFileNameDatetimeFormatter));
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

    private IndexedFileEntity saveNewFile(WrappedExternalFileServingRequestDto wrappedExternalFileServingRequestDto,
                                          InputStream fileUploadingInputStream, String uploadedFileName, Long approximateFileSize) {
        StoredFile newUploadableFile = null;
        try {
            newUploadableFile = uploadableFileFactory.fromFileUploadRequest(wrappedExternalFileServingRequestDto, uploadedFileName);

            TrackingInputStream trackingInputStream = trackingInputStreamFactory.createForFileUpload(fileUploadingInputStream);
            trackingInputStream.setLimit(uploadFileMaxSize);

            newUploadableFile.saveFromStream(trackingInputStream, approximateFileSize);
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
            log.error("Exception while marking uploaded file as {}", statusToMarkAs, ex);
        }
    }

    private void cleanUpFile(IndexedFileStatus statusAfterCleanup, StoredFile storedFile) {
        try {
            if (storedFile != null) {
                String fullPath = storedFileUtils.getFullPathFromStoredFileEntityPath(storedFile.getIndexedFileEntity().getPath());
                java.nio.file.Files.deleteIfExists(Path.of(fullPath));

                storedFile.getIndexedFileEntity().setStatus(statusAfterCleanup);
                indexedFileEntityRepository.save(storedFile.getIndexedFileEntity());
                log.trace("File {} cleaned up: {}", storedFile.getIndexedFileEntity().getId(), statusAfterCleanup);
            }
        } catch (Exception ex) {
            log.error("Couldn't clean up file after QuotaExceededException!", ex);
        }
    }
}
