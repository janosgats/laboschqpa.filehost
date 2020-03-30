package com.laboschqpa.filehost.model;

import java.time.Instant;

public interface ServiceableFile extends ServiceableStream, IndexedFile {
    /**
     * Gets the file's creation time.
     *
     * @return {@code Null}, if creation time is unavailable.
     */
    Instant getCreationTime();

    /**
     * Gets the file's update time.
     *
     * @return {@code Null}, if update time is unavailable.
     */
    Instant getUpdateTime();

    /**
     * Gets the original file name.
     *
     * @return {@code Null}, if original file name is unavailable.
     */
    String getOriginalFileName();
}
