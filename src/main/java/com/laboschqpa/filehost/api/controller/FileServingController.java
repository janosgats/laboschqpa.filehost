package com.laboschqpa.filehost.api.controller;

import com.laboschqpa.filehost.api.service.FileServingService;
import com.laboschqpa.filehost.config.filter.WrappedFileServingHttpServletRequest;
import com.laboschqpa.filehost.enums.FileAccessType;
import lombok.RequiredArgsConstructor;
import org.apache.tomcat.util.http.fileupload.FileUploadException;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/file")
public class FileServingController {
    private final FileServingService fileServingService;

    @GetMapping("/**")
    public ResponseEntity<Resource> getDownload(WrappedFileServingHttpServletRequest request) {
        if (request.getWrappedFileServingRequestDto().getFileAccessType() != FileAccessType.READ) {
            throw new IllegalStateException("FileAccessType shouldn't be "
                    + request.getWrappedFileServingRequestDto().getFileAccessType() + "!");
        }

        return fileServingService.downloadFile(request);
    }

    @PostMapping("/**")
    public Long postUpload(WrappedFileServingHttpServletRequest request) throws IOException, FileUploadException {
        if (request.getWrappedFileServingRequestDto().getFileAccessType() != FileAccessType.WRITE) {
            throw new IllegalStateException("FileAccessType shouldn't be "
                    + request.getWrappedFileServingRequestDto().getFileAccessType() + "!");
        }

        return fileServingService.uploadFile(request).getId();
    }

    @DeleteMapping("/**")
    public void deleteDelete(WrappedFileServingHttpServletRequest request) throws IOException, FileUploadException {
        if (request.getWrappedFileServingRequestDto().getFileAccessType() != FileAccessType.DELETE) {
            throw new IllegalStateException("FileAccessType shouldn't be "
                    + request.getWrappedFileServingRequestDto().getFileAccessType() + "!");
        }
    }
}
