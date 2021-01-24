package com.laboschqpa.filehost.model.file;

import com.laboschqpa.filehost.entity.LocalDiskFileEntity;
import com.laboschqpa.filehost.enums.IndexedFileStatus;
import com.laboschqpa.filehost.enums.apierrordescriptor.FileServingApiError;
import com.laboschqpa.filehost.exceptions.apierrordescriptor.FileServingException;
import com.laboschqpa.filehost.model.inputstream.CountingInputStream;
import com.laboschqpa.filehost.service.LocalDiskFileSaver;
import com.laboschqpa.filehost.util.LocalDiskFileUtils;
import lombok.extern.log4j.Log4j2;
import org.apache.tika.detect.Detector;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Objects;

@Log4j2
public class LocalDiskFile implements DownloadableFile, DeletableFile, UploadableFile {
    private final LocalDiskFileUtils localDiskFileUtils;
    private final LocalDiskFileEntity localDiskFileEntity;
    private final LocalDiskFileSaver localDiskFileSaver;
    private final Detector tikaDetector;
    private File file;

    private FileInputStream readingStream;


    public LocalDiskFile(LocalDiskFileUtils localDiskFileUtils, LocalDiskFileEntity localDiskFileEntity, LocalDiskFileSaver localDiskFileSaver, Detector tikaDetector) {
        this(localDiskFileUtils, localDiskFileEntity, localDiskFileSaver, tikaDetector, true);
    }

    public LocalDiskFile(LocalDiskFileUtils localDiskFileUtils, LocalDiskFileEntity localDiskFileEntity, LocalDiskFileSaver localDiskFileSaver, Detector tikaDetector, boolean assertFileCurrentlyExists) {
        Objects.requireNonNull(localDiskFileUtils);
        Objects.requireNonNull(localDiskFileEntity);

        this.localDiskFileUtils = localDiskFileUtils;
        this.localDiskFileEntity = localDiskFileEntity;
        this.localDiskFileSaver = localDiskFileSaver;
        this.tikaDetector = tikaDetector;

        file = new File(localDiskFileUtils.getFullPathFromStoredFileEntityPath(localDiskFileEntity.getPath()));

        if (assertFileCurrentlyExists) {
            if (!isFileCurrentlyExisting())
                throw new FileServingException(FileServingApiError.INVALID_STORED_FILE, "File from storedFileEntity is not a valid file: " + file.getAbsolutePath());
        }
    }

    @Override
    public Long getSize() {
        return localDiskFileEntity.getSize();
    }

    @Override
    public void saveFromStream(CountingInputStream fileUploadingInputStream) {
        localDiskFileEntity.setStatus(IndexedFileStatus.UPLOADING);
        localDiskFileUtils.saveLocalDiskFileEntity(localDiskFileEntity);

        localDiskFileSaver.writeFromStream(fileUploadingInputStream, file, localDiskFileEntity);

        try (TikaInputStream tikaInputStream = TikaInputStream.get(Path.of(file.getAbsolutePath()))) {
            Metadata metadata = new Metadata();
            metadata.add(Metadata.RESOURCE_NAME_KEY, localDiskFileEntity.getOriginalFileName());
            MediaType detectedMediaType = tikaDetector.detect(tikaInputStream, metadata);

            log.trace("Detected MIME type: {}", detectedMediaType.toString());

            localDiskFileEntity.setMimeType(detectedMediaType.getType() + "/" + detectedMediaType.getSubtype());
        } catch (Exception e) {
            log.error("Exception during Apache Tika mime type detection!", e);
        }

        localDiskFileEntity.setStatus(IndexedFileStatus.AVAILABLE);
        localDiskFileUtils.saveLocalDiskFileEntity(localDiskFileEntity);
    }

    @Override
    public void cleanUpFailedUpload() {
        String fullPath = localDiskFileUtils.getFullPathFromStoredFileEntityPath(localDiskFileEntity.getPath());
        try {
            java.nio.file.Files.deleteIfExists(Path.of(fullPath));
        } catch (IOException e) {
            throw new RuntimeException("IOException while deleting LocalDiskFile after failed upload: " + fullPath, e);
        }
        log.trace("File {} cleaned up after failed upload.", localDiskFileEntity.getId());
    }

    @Override
    public InputStream getStream() {
        if (!isFileCurrentlyExisting())
            throw new FileServingException(FileServingApiError.FILE_DOES_NOT_EXIST, "File does not exist currently!");

        if (!isAvailable())
            throw new FileServingException(FileServingApiError.FILE_IS_NOT_AVAILABLE, "The file is not available (yet)!");

        if (readingStream == null) {
            try {
                this.readingStream = new FileInputStream(file);
            } catch (Exception e) {
                throw new FileServingException(FileServingApiError.CANNOT_CREATE_FILE_READ_STREAM, "Cannot instantiate FileInputStream from StoredFile::file!", e);
            }
        }

        return readingStream;
    }

    @Override
    public String getETag() {
        return String.format("\"%s_%s\"",
                localDiskFileEntity.getCreationTime().getEpochSecond(),
                localDiskFileEntity.getStatus().getValue().toString()
        );
    }

    @Override
    public Instant getCreationTime() {
        return localDiskFileEntity.getCreationTime();
    }

    @Override
    public String getOriginalFileName() {
        return localDiskFileEntity.getOriginalFileName();
    }

    @Override
    public String getMimeType() {
        return localDiskFileEntity.getMimeType();
    }

    @Override
    public boolean isAvailable() {
        return localDiskFileEntity.getStatus() == IndexedFileStatus.AVAILABLE;
    }

    @Override
    public void delete() {
        if (!isFileCurrentlyExisting())
            throw new FileServingException(FileServingApiError.FILE_DOES_NOT_EXIST, "File does not exist currently!");

        try {
            localDiskFileEntity.setStatus(IndexedFileStatus.DELETED);
            localDiskFileUtils.saveLocalDiskFileEntity(localDiskFileEntity);
            java.nio.file.Files.delete(Path.of(file.getAbsolutePath()));
        } catch (IOException e) {
            log.error("Couldn't delete file {}!", localDiskFileEntity.getId(), e);
            localDiskFileEntity.setStatus(IndexedFileStatus.FAILED_DURING_DELETION);
            localDiskFileUtils.saveLocalDiskFileEntity(localDiskFileEntity);
            throw new FileServingException(FileServingApiError.CANNOT_DELETE_FILE, "Cannot delete file: " + file.getAbsolutePath(), e);
        }
    }

    private boolean isFileCurrentlyExisting() {
        return file != null && file.exists() && file.isFile();
    }

    @Override
    public LocalDiskFileEntity getIndexedFileEntity() {
        return localDiskFileEntity;
    }
}
