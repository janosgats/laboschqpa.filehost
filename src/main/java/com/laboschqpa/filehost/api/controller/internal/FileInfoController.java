package com.laboschqpa.filehost.api.controller.internal;

import com.laboschqpa.filehost.api.dto.GetIndexedFileInfoResultDto;
import com.laboschqpa.filehost.api.service.FileInfoService;
import com.laboschqpa.filehost.config.filter.AuthWrappedHttpServletRequest;
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
    public List<GetIndexedFileInfoResultDto> getIndexedFileInfo(AuthWrappedHttpServletRequest request,
                                                                @RequestBody List<Long> indexedFileIds) {
        request.assertIsAuthInterServiceCall();
        return fileInfoService.getIndexedFileInfo(indexedFileIds);
    }
}
