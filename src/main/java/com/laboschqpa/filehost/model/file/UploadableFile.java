package com.laboschqpa.filehost.model.file;

import com.laboschqpa.filehost.model.inputstream.CountingInputStream;

public interface UploadableFile extends IndexedFile {

    /**
     * Writes the given stream to the file. (Overwriting)
     */
    void saveFromStream(CountingInputStream fileUploadInputStream);

    /**
     * Performs post-upload cleanup tasks in case of failure.
     */
    void cleanUpFailedUpload();
}
