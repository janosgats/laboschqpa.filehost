package com.laboschqpa.filehost.api.controller;

import com.laboschqpa.filehost.api.service.FileDownloaderService;
import com.laboschqpa.filehost.api.service.FileUploaderService;
import com.laboschqpa.filehost.config.filter.AuthWrappedHttpServletRequest;
import com.laboschqpa.filehost.enums.FileAccessType;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/file")
public class FileServingController {
    private final FileDownloaderService fileDownloaderService;
    private final FileUploaderService fileUploaderService;

    @GetMapping("/**")
    public ResponseEntity<Resource> getDownload(AuthWrappedHttpServletRequest request) {
        if (request.getWrappedExternalFileServingRequestDto().getFileAccessType() != FileAccessType.READ) {
            throw new IllegalStateException("FileAccessType shouldn't be "
                    + request.getWrappedExternalFileServingRequestDto().getFileAccessType() + "!");
        }

        return fileDownloaderService.downloadFile(request);
    }

    @PostMapping("/**")
    public Long postUpload(AuthWrappedHttpServletRequest request) {
        if (request.getWrappedExternalFileServingRequestDto().getFileAccessType() != FileAccessType.CREATE_NEW) {
            throw new IllegalStateException("FileAccessType shouldn't be "
                    + request.getWrappedExternalFileServingRequestDto().getFileAccessType() + "!");
        }

        return fileUploaderService.uploadFile(request).getId();
    }
}
