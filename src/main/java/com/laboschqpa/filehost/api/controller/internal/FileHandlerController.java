package com.laboschqpa.filehost.api.controller.internal;

import com.laboschqpa.filehost.api.service.FileDeleterService;
import com.laboschqpa.filehost.config.filter.AuthWrappedHttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/internal/fileHandler")
public class FileHandlerController {
    private final FileDeleterService fileDeleterService;

    @DeleteMapping("/delete")
    public void deleteDeleteFile(AuthWrappedHttpServletRequest request, @RequestParam("id") Long fileIdToDelete) {
        request.assertIsAuthInterServiceCall();
        fileDeleterService.deleteFile(fileIdToDelete);
    }
}
