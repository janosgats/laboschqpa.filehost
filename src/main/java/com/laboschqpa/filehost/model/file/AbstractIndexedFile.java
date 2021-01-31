package com.laboschqpa.filehost.model.file;

import com.laboschqpa.filehost.entity.IndexedFileEntity;
import com.laboschqpa.filehost.enums.IndexedFileStatus;

import java.time.Instant;

public class AbstractIndexedFile<T extends IndexedFileEntity> implements IndexedFile {
    protected final T indexedFileEntity;

    public AbstractIndexedFile(T indexedFileEntity) {
        this.indexedFileEntity = indexedFileEntity;
    }

    @Override
    public Instant getCreationTime() {
        return indexedFileEntity.getCreationTime();
    }

    @Override
    public String getOriginalFileName() {
        return indexedFileEntity.getOriginalFileName();
    }

    @Override
    public String getMimeType() {
        return indexedFileEntity.getMimeType();
    }

    @Override
    public boolean isAvailable() {
        return indexedFileEntity.getStatus() == IndexedFileStatus.AVAILABLE;
    }

    @Override
    public IndexedFileStatus getStatus() {
        return indexedFileEntity.getStatus();
    }

    @Override
    public Long getSize() {
        return indexedFileEntity.getSize();
    }

    @Override
    public T getEntity() {
        return indexedFileEntity;
    }
}
