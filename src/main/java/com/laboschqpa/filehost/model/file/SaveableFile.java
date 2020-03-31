package com.laboschqpa.filehost.model.file;

import com.laboschqpa.filehost.model.streamtracking.TrackingInputStream;

public interface SaveableFile extends IndexedFile {
    /**
     * Should be called after the file is saved.
     *
     * @return The written stream length in bytes.
     */
    Long getSize();

    /**
     * Writes the given stream to the file. (Overwriting)
     */
    void saveFromStream(TrackingInputStream fileUploadInputStream);
}
