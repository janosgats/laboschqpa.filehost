package com.laboschqpa.filehost.model;

import com.laboschqpa.filehost.entity.StoredFileEntity;
import com.laboschqpa.filehost.enums.StoredFileStatus;
import com.laboschqpa.filehost.exceptions.fileserving.FileServingException;
import com.laboschqpa.filehost.exceptions.fileserving.InvalidStoredFileException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.time.Instant;

public class StoredFile implements ServiceableFile, DeletableFile {
    private final String storedFilesBasePath;

    private final StoredFileEntity storedFileEntity;
    private final File file;

    private FileInputStream fileInputStream;


    public StoredFile(String storedFilesBasePath, StoredFileEntity storedFileEntity) {
        this.storedFilesBasePath = storedFilesBasePath;
        this.storedFileEntity = storedFileEntity;

        file = new File(getFullPathFromStoredFileEntityPath(storedFileEntity.getPath()));

        if (!file.exists() || !file.isFile())
            throw new InvalidStoredFileException("File from storedFileEntity is not a valid file: " + file.getAbsolutePath());
    }

    private String getFullPathFromStoredFileEntityPath(String storedFileEntityPath) {
        return Path.of(storedFilesBasePath, storedFileEntityPath).toString();
    }

    @Override
    public Long getSize() {
        return storedFileEntity.getSize();
    }

    @Override
    public InputStream getStream() {
        if (fileInputStream == null) {
            try {
                this.fileInputStream = new FileInputStream(file);
            } catch (Exception e) {
                throw new FileServingException("Cannot instantiate FileInputStream from storedFileEntity!", e);
            }
        }

        return fileInputStream;
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
    public boolean isAvailable() {
        return storedFileEntity.getStatus() == StoredFileStatus.AVAILABLE;
    }

    @Override
    public void delete() {
        try {
            java.nio.file.Files.delete(Path.of(file.getAbsolutePath()));
        } catch (IOException e) {
            throw new FileServingException("Cannot delete file: " + file.getAbsolutePath(), e);
        }
    }
}
