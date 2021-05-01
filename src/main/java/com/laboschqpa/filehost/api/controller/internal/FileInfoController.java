package com.laboschqpa.filehost.api.controller.internal;

import com.laboschqpa.filehost.api.dto.GetIndexedFileInfoResponse;
import com.laboschqpa.filehost.api.service.FileInfoService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/internal/fileInfo")
public class FileInfoController {
    private final FileInfoService fileInfoService;

    @GetMapping("/indexedFileInfo")
    public List<GetIndexedFileInfoResponse> getIndexedFileInfo(@RequestBody List<Long> indexedFileIds) {
        return fileInfoService.getIndexedFileInfo(indexedFileIds);
    }
}
