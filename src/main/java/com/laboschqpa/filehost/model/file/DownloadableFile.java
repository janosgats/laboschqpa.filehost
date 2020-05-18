package com.laboschqpa.filehost.model.file;

import java.io.InputStream;
import java.time.Instant;

public interface DownloadableFile extends IndexedFile {
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
     * Gets the original file name.
     *
     * @return {@code Null}, if original file name is unavailable.
     */
    String getOriginalFileName();

    /**
     * Gets the file's MIME type if possible.
     *
     * @return {@code Null}, if MIME type is unknown.
     */
    String getMimeType();
}
