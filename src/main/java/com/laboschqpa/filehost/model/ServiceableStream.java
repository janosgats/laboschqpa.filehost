package com.laboschqpa.filehost.model;

import java.io.InputStream;

public interface ServiceableStream {
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
}
