package com.laboschqpa.filehost.api.controller.exposed;

import com.laboschqpa.filehost.api.dto.FileUploadResponseDto;
import com.laboschqpa.filehost.api.service.FileDownloaderService;
import com.laboschqpa.filehost.api.service.FileUploaderService;
import com.laboschqpa.filehost.config.AppConstants;
import com.laboschqpa.filehost.entity.IndexedFileEntity;
import com.laboschqpa.filehost.enums.FileAccessType;
import com.laboschqpa.filehost.model.upload.FileUploadRequest;
import com.laboschqpa.filehost.service.fileservingauth.AuthorizeRequestResult;
import com.laboschqpa.filehost.service.fileservingauth.FileServingUserAuthorizerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

@Log4j2
@RequiredArgsConstructor
@RestController
@RequestMapping(AppConstants.userAccessibleBaseUrl + "/file")
public class FileServingController {
    private final FileServingUserAuthorizerService fileServingUserAuthorizerService;

    private final FileDownloaderService fileDownloaderService;
    private final FileUploaderService fileUploaderService;

    @GetMapping("/**")
    public ResponseEntity<Resource> getDownload(@RequestParam("id") Long fileId, HttpServletRequest httpServletRequest) {
        fileServingUserAuthorizerService.authorizeRequestOrThrow(fileId, FileAccessType.READ, httpServletRequest);

        return fileDownloaderService.downloadFile(fileId, httpServletRequest);
    }

    @PostMapping("/**")
    public FileUploadResponseDto postUpload(HttpServletRequest httpServletRequest) {
        final AuthorizeRequestResult authorizeRequestReturn
                = fileServingUserAuthorizerService.authorizeRequestOrThrow(null, FileAccessType.CREATE_NEW, httpServletRequest);

        final FileUploadRequest fileUploadRequest
                = new FileUploadRequest(authorizeRequestReturn.getLoggedInUserId(), authorizeRequestReturn.getLoggedInUserTeamId());

        IndexedFileEntity createdFile = fileUploaderService.uploadFile(fileUploadRequest, httpServletRequest);
        return new FileUploadResponseDto(createdFile.getId());
    }
}
