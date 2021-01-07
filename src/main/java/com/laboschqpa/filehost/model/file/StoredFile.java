package com.laboschqpa.filehost.model.file;

import com.laboschqpa.filehost.entity.StoredFileEntity;
import com.laboschqpa.filehost.enums.IndexedFileStatus;
import com.laboschqpa.filehost.enums.apierrordescriptor.FileServingApiError;
import com.laboschqpa.filehost.exceptions.apierrordescriptor.FileServingException;
import com.laboschqpa.filehost.model.streamtracking.TrackingInputStream;
import com.laboschqpa.filehost.service.StoredFileSaver;
import com.laboschqpa.filehost.util.StoredFileUtils;
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
public class StoredFile implements DownloadableFile, DeletableFile, UploadableFile {
    private final StoredFileUtils storedFileUtils;
    private final StoredFileEntity storedFileEntity;
    private final StoredFileSaver storedFileSaver;
    private final Detector tikaDetector;
    private File file;

    private FileInputStream readingStream;


    public StoredFile(StoredFileUtils storedFileUtils, StoredFileEntity storedFileEntity, StoredFileSaver storedFileSaver, Detector tikaDetector) {
        this(storedFileUtils, storedFileEntity, storedFileSaver, tikaDetector, true);
    }

    public StoredFile(StoredFileUtils storedFileUtils, StoredFileEntity storedFileEntity, StoredFileSaver storedFileSaver, Detector tikaDetector, boolean assertFileCurrentlyExists) {
        Objects.requireNonNull(storedFileUtils);
        Objects.requireNonNull(storedFileEntity);

        this.storedFileUtils = storedFileUtils;
        this.storedFileEntity = storedFileEntity;
        this.storedFileSaver = storedFileSaver;
        this.tikaDetector = tikaDetector;

        file = new File(storedFileUtils.getFullPathFromStoredFileEntityPath(storedFileEntity.getPath()));

        if (assertFileCurrentlyExists) {
            if (!isFileCurrentlyExisting())
                throw new FileServingException(FileServingApiError.INVALID_STORED_FILE, "File from storedFileEntity is not a valid file: " + file.getAbsolutePath());
        }
    }

    @Override
    public Long getSize() {
        return storedFileEntity.getSize();
    }

    @Override
    public void saveFromStream(TrackingInputStream fileUploadingInputStream, Long approximateFileSize) {
        storedFileEntity.setStatus(IndexedFileStatus.UPLOADING);
        storedFileUtils.saveStoredFileEntity(storedFileEntity);

        storedFileSaver.writeFromStream(fileUploadingInputStream, file, storedFileEntity, approximateFileSize);

        try (TikaInputStream tikaInputStream = TikaInputStream.get(Path.of(file.getAbsolutePath()))) {
            Metadata metadata = new Metadata();
            metadata.add(Metadata.RESOURCE_NAME_KEY, storedFileEntity.getOriginalFileName());
            MediaType detectedMediaType = tikaDetector.detect(tikaInputStream, metadata);

            log.trace("Detected MIME type: {}", detectedMediaType.toString());

            storedFileEntity.setMimeType(detectedMediaType.getType() + "/" + detectedMediaType.getSubtype());
        } catch (Exception e) {
            log.error("Exception during Apache Tika mime type detection!", e);
        }

        storedFileEntity.setStatus(IndexedFileStatus.AVAILABLE);
        storedFileUtils.saveStoredFileEntity(storedFileEntity);
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
                storedFileEntity.getCreationTime().getEpochSecond(),
                storedFileEntity.getStatus().getValue().toString()
        );
    }

    @Override
    public Instant getCreationTime() {
        return storedFileEntity.getCreationTime();
    }

    @Override
    public Instant getUpdateTime() {
        return storedFileEntity.getCreationTime();
    }

    @Override
    public String getOriginalFileName() {
        return storedFileEntity.getOriginalFileName();
    }

    @Override
    public String getMimeType() {
        return storedFileEntity.getMimeType();
    }

    @Override
    public boolean isAvailable() {
        return storedFileEntity.getStatus() == IndexedFileStatus.AVAILABLE;
    }

    @Override
    public void delete() {
        if (!isFileCurrentlyExisting())
            throw new FileServingException(FileServingApiError.FILE_DOES_NOT_EXIST, "File does not exist currently!");

        try {
            storedFileEntity.setStatus(IndexedFileStatus.DELETED);
            storedFileUtils.saveStoredFileEntity(storedFileEntity);
            java.nio.file.Files.delete(Path.of(file.getAbsolutePath()));
        } catch (IOException e) {
            log.error("Couldn't delete file {}!", storedFileEntity.getId(), e);
            storedFileEntity.setStatus(IndexedFileStatus.FAILED_DURING_DELETION);
            storedFileUtils.saveStoredFileEntity(storedFileEntity);
            throw new FileServingException(FileServingApiError.CANNOT_DELETE_FILE, "Cannot delete file: " + file.getAbsolutePath(), e);
        }
    }

    private boolean isFileCurrentlyExisting() {
        return file != null && file.exists() && file.isFile();
    }

    @Override
    public StoredFileEntity getIndexedFileEntity() {
        return storedFileEntity;
    }
}
