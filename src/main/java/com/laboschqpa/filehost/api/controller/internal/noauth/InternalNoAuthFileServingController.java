package com.laboschqpa.filehost.api.controller.internal.noauth;

import com.laboschqpa.filehost.api.service.FileDownloaderService;
import com.laboschqpa.filehost.config.AppConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

@Log4j2
@RequiredArgsConstructor
@RestController
@RequestMapping(AppConstants.internalNoAuthBaseUrl + "/file")
public class InternalNoAuthFileServingController {
    private final FileDownloaderService fileDownloaderService;


    @GetMapping("/downloadOriginal")
    public ResponseEntity<Resource> getDownloadOriginalWithParam(@RequestParam("id") Long fileId, HttpServletRequest httpServletRequest) {
        return downloadOriginal(fileId, httpServletRequest);
    }

    @GetMapping("/downloadOriginal/{id}")
    public ResponseEntity<Resource> getDownloadOriginalWithSlug(@PathVariable("id") Long fileId, HttpServletRequest httpServletRequest) {
        return downloadOriginal(fileId, httpServletRequest);
    }

    private ResponseEntity<Resource> downloadOriginal(Long fileId, HttpServletRequest httpServletRequest) {
        return fileDownloaderService.downloadFile(fileId, httpServletRequest);
    }
}
