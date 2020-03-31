package com.laboschqpa.filehost.model.file;

import com.laboschqpa.filehost.entity.StoredFileEntity;
import com.laboschqpa.filehost.enums.IndexedFileStatus;
import com.laboschqpa.filehost.exceptions.fileserving.FileServingException;
import com.laboschqpa.filehost.exceptions.fileserving.InvalidStoredFileException;
import com.laboschqpa.filehost.model.streamtracking.TrackingInputStream;
import com.laboschqpa.filehost.util.StoredFileUtils;

import java.io.*;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Objects;

public class StoredFile implements DownloadableFile, DeletableFile, SaveableFile {
    private final StoredFileUtils storedFileUtils;
    private final StoredFileEntity storedFileEntity;

    private File file;

    private FileInputStream readingStream;


    public StoredFile(StoredFileUtils storedFileUtils, StoredFileEntity storedFileEntity) {
        this(storedFileUtils, storedFileEntity, true);
    }

    public StoredFile(StoredFileUtils storedFileUtils, StoredFileEntity storedFileEntity, boolean assertFileCurrentlyExists) {
        Objects.requireNonNull(storedFileUtils);
        Objects.requireNonNull(storedFileEntity);

        this.storedFileUtils = storedFileUtils;
        this.storedFileEntity = storedFileEntity;

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
    public void saveFromStream(TrackingInputStream fileUploadingInputStream) {
        storedFileEntity.setStatus(IndexedFileStatus.PROCESSING);
        storedFileUtils.saveStoredFileEntity(storedFileEntity);

        storedFileUtils.writeWholeStreamToFile(fileUploadingInputStream, file);

        storedFileEntity.setSize(fileUploadingInputStream.getCountOfReadBytes());
        storedFileEntity.setStatus(IndexedFileStatus.AVAILABLE);

        storedFileUtils.saveStoredFileEntity(storedFileEntity);
    }

    @Override
    public InputStream getStream() {
        if (!isFileCurrentlyExisting())
            throw new FileServingException("File is not existing currently!");

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
            throw new FileServingException("File is not existing currently!");

        try {
            storedFileEntity.setStatus(IndexedFileStatus.DELETED);
            storedFileUtils.saveStoredFileEntity(storedFileEntity);
            java.nio.file.Files.delete(Path.of(file.getAbsolutePath()));
        } catch (IOException e) {
            storedFileEntity.setStatus(IndexedFileStatus.FAILURE);
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
