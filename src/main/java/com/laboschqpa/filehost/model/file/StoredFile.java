package com.laboschqpa.filehost.model.file;

import com.laboschqpa.filehost.entity.StoredFileEntity;
import com.laboschqpa.filehost.enums.IndexedFileStatus;
import com.laboschqpa.filehost.exceptions.fileserving.FileServingException;
import com.laboschqpa.filehost.exceptions.fileserving.InvalidStoredFileException;
import com.laboschqpa.filehost.model.streamtracking.TrackingInputStream;
import com.laboschqpa.filehost.service.StoredFileSaver;
import com.laboschqpa.filehost.util.StoredFileUtils;

import java.io.*;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Objects;

public class StoredFile implements DownloadableFile, DeletableFile, UploadableFile {
    private final StoredFileUtils storedFileUtils;
    private final StoredFileEntity storedFileEntity;
    private final StoredFileSaver storedFileSaver;

    private File file;

    private FileInputStream readingStream;


    public StoredFile(StoredFileUtils storedFileUtils, StoredFileEntity storedFileEntity, StoredFileSaver storedFileSaver) {
        this(storedFileUtils, storedFileEntity, storedFileSaver, true);
    }

    public StoredFile(StoredFileUtils storedFileUtils, StoredFileEntity storedFileEntity, StoredFileSaver storedFileSaver, boolean assertFileCurrentlyExists) {
        Objects.requireNonNull(storedFileUtils);
        Objects.requireNonNull(storedFileEntity);

        this.storedFileUtils = storedFileUtils;
        this.storedFileEntity = storedFileEntity;
        this.storedFileSaver = storedFileSaver;

        file = new File(storedFileUtils.getFullPathFromStoredFileEntityPath(storedFileEntity.getPath()));

        if (assertFileCurrentlyExists) {
            if (!isFileCurrentlyExisting())
                throw new InvalidStoredFileException("File from storedFileEntity is not a valid file: " + file.getAbsolutePath());
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

        storedFileEntity.setStatus(IndexedFileStatus.AVAILABLE);
        storedFileUtils.saveStoredFileEntity(storedFileEntity);
    }

    @Override
    public InputStream getStream() {
        if (!isFileCurrentlyExisting())
            throw new FileServingException("File does not exist currently!");

        if (!isAvailable())
            throw new FileServingException("The file is not available (yet)!");

        if (readingStream == null) {
            try {
                this.readingStream = new FileInputStream(file);
            } catch (Exception e) {
                throw new FileServingException("Cannot instantiate FileInputStream from StoredFile::file!", e);
            }
        }

        return readingStream;
    }

    @Override
    public String getETag() {
        return storedFileEntity.getETag();
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
    public boolean isAvailable() {
        return storedFileEntity.getStatus() == IndexedFileStatus.AVAILABLE;
    }

    @Override
    public void delete() {
        if (!isFileCurrentlyExisting())
            throw new FileServingException("File does not exist currently!");

        try {
            storedFileEntity.setStatus(IndexedFileStatus.DELETED);
            storedFileUtils.saveStoredFileEntity(storedFileEntity);
            java.nio.file.Files.delete(Path.of(file.getAbsolutePath()));
        } catch (IOException e) {
            storedFileEntity.setStatus(IndexedFileStatus.FAILED);
            storedFileUtils.saveStoredFileEntity(storedFileEntity);
            throw new FileServingException("Cannot delete file: " + file.getAbsolutePath(), e);
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
