package com.laboschqpa.filehost.model;

import com.laboschqpa.filehost.entity.IndexedFileEntity;
import com.laboschqpa.filehost.enums.IndexedFileStatus;

public interface IndexedFile {
    /**
     * @return The corresponding {@link IndexedFileEntity}.
     */
    IndexedFileEntity getIndexedFileEntity();

    /**
     * Gets if the file is available for download. E.g. it's not under postprocessing.
     */
    boolean isAvailable();

    /**
     * Gets the {@link IndexedFileStatus} of the file.
     */
    default IndexedFileStatus getStatus(){
        return getIndexedFileEntity().getStatus();
    }
}
