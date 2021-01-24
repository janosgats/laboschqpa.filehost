package com.laboschqpa.filehost.model.file;

import java.io.InputStream;

public interface UploadableFile extends IndexedFile {

    /**
     * Writes the given stream to the file. (Overwriting)
     */
    void saveFromStream(InputStream fileUploadInputStream);

    /**
     * Performs post-upload cleanup tasks in case of failure.
     */
    void cleanUpFailedUpload();
}
