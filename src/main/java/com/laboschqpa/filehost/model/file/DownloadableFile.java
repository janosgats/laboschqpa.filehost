package com.laboschqpa.filehost.model.file;

import java.io.InputStream;

//TODO: Replace this with HttpServableFile - which fills an HTTP response entity by itself
public interface DownloadableFile extends IndexedFile {
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
