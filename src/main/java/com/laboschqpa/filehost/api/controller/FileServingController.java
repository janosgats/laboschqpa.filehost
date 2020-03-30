package com.laboschqpa.filehost.api.controller;

import com.laboschqpa.filehost.api.service.FileServingService;
import com.laboschqpa.filehost.config.filter.FileServingHttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.apache.tomcat.util.http.fileupload.FileUploadException;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/file")
public class FileServingController {
    private final FileServingService fileServingService;

    @GetMapping("/**")
    public ResponseEntity<Resource> getDownload(FileServingHttpServletRequest request) throws IOException, FileUploadException {
        return fileServingService.downloadFile(request);
    }

    @PostMapping("/**")
    public void postUpload(FileServingHttpServletRequest request) throws IOException, FileUploadException {
        fileServingService.uploadFile(request);
    }
}
