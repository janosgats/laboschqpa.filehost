package com.laboschqpa.filehost.api.controller.internal;

import com.laboschqpa.filehost.api.service.FileDeleterService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/internal/fileHandler")
public class FileHandlerController {
    private final FileDeleterService fileDeleterService;

    @DeleteMapping("/delete")
    public void deleteDeleteFile(@RequestParam("id") Long fileIdToDelete) {
        fileDeleterService.deleteFile(fileIdToDelete);
    }
}
