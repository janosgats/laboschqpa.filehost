package com.laboschqpa.filehost.model;

public interface DeletableFile extends IndexedFile {
    /**
     * Permanently deletes the file.
     */
    void delete();
}
