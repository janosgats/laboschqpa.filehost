package com.laboschqpa.filehost.model.file;

import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;

import javax.servlet.http.HttpServletRequest;

public interface HttpServableFile extends IndexedFile {
    /**
     * "Puts" the file into the HTTP response
     */
    ResponseEntity<Resource> getDownloadResponseEntity(HttpServletRequest request);
}
