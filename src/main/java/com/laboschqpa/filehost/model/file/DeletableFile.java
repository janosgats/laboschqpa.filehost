package com.laboschqpa.filehost.model.file;

public interface DeletableFile extends IndexedFile {
    /**
     * Permanently deletes the file.
     */
    void delete();
}
