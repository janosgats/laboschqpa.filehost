package com.laboschqpa.filehost.model;

import java.io.InputStream;
import java.time.Instant;

public interface ServiceableFile {
    /**
     * Gets the stream length in bytes.
     *
     * @return {@code Null}, if length is unavailable.
     */
    Long getSize();

    /**
     * Gets the file's content.
     */
    InputStream getStream();

    /**
     * Gets the file's eTag value.
     *
     * @return {@code Null}, if eTag value is unavailable.
     */
    String getETag();

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
     * Gets if the file is available for download. E.g. it's not under postprocessing.
     */
    boolean isAvailable();

}
