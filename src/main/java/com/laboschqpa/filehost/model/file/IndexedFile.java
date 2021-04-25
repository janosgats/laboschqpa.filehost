package com.laboschqpa.filehost.model.file;

import com.laboschqpa.filehost.entity.IndexedFileEntity;
import com.laboschqpa.filehost.enums.IndexedFileStatus;

import java.time.Instant;

public interface IndexedFile {
    /**
     * @return The corresponding {@link IndexedFileEntity}.
     */
    IndexedFileEntity getEntity();

    /**
     * Gets if the file is available for download. E.g. it's not under postprocessing.
     */
    boolean isAvailable();

    /**
     * Gets the {@link IndexedFileStatus} of the file.
     */
    default IndexedFileStatus getStatus(){
        return getEntity().getStatus();
    }

    /**
     * Gets the stream length in bytes.
     *
     * @return {@code Null}, if length is unavailable.
     */
    Long getSize();

    /**
     * Gets file name.
     *
     * @return {@code Null}, if original file name is unavailable.
     */
    String getFileName();

    /**
     * Gets the file's MIME type if possible.
     *
     * @return {@code Null}, if MIME type is unknown.
     */
    String getMimeType();

    /**
     * Gets the file's creation time.
     *
     * @return {@code Null}, if creation time is unavailable.
     */
    Instant getCreationTime();
}
