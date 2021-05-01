package com.laboschqpa.filehost.api.controller.exposed;

import com.laboschqpa.filehost.api.dto.FileUploadResponse;
import com.laboschqpa.filehost.api.service.FileDownloaderService;
import com.laboschqpa.filehost.api.service.FileUploaderService;
import com.laboschqpa.filehost.config.AppConstants;
import com.laboschqpa.filehost.entity.IndexedFileEntity;
import com.laboschqpa.filehost.enums.FileAccessType;
import com.laboschqpa.filehost.enums.UploadedFileType;
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

    @PostMapping("/any/**")
    public FileUploadResponse postUploadAny(HttpServletRequest httpServletRequest) {
        IndexedFileEntity createdFile = upload(httpServletRequest, UploadedFileType.ANY);
        return new FileUploadResponse(createdFile.getId(), createdFile.getMimeType());
    }

    @PostMapping("/image/**")
    public FileUploadResponse postUploadImage(HttpServletRequest httpServletRequest) {
        IndexedFileEntity createdFile = upload(httpServletRequest, UploadedFileType.IMAGE);
        return new FileUploadResponse(createdFile.getId(), createdFile.getMimeType());
    }

    private IndexedFileEntity upload(HttpServletRequest httpServletRequest, UploadedFileType forcedFileType) {
        final AuthorizeRequestResult authorizeRequestReturn
                = fileServingUserAuthorizerService.authorizeRequestOrThrow(null, FileAccessType.CREATE_NEW, httpServletRequest);

        final FileUploadRequest fileUploadRequest
                = new FileUploadRequest(authorizeRequestReturn.getLoggedInUserId(),
                authorizeRequestReturn.getLoggedInUserTeamId(), forcedFileType);

        return fileUploaderService.uploadFile(fileUploadRequest, httpServletRequest);
    }
}
