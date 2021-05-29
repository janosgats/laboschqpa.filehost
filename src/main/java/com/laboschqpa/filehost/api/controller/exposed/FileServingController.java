package com.laboschqpa.filehost.api.controller.exposed;

import com.laboschqpa.filehost.api.dto.FileUploadResponse;
import com.laboschqpa.filehost.api.service.FileDownloaderService;
import com.laboschqpa.filehost.api.service.FileUploaderService;
import com.laboschqpa.filehost.config.AppConstants;
import com.laboschqpa.filehost.entity.IndexedFileEntity;
import com.laboschqpa.filehost.enums.FileAccessType;
import com.laboschqpa.filehost.enums.FileServingEventKind;
import com.laboschqpa.filehost.enums.UploadKind;
import com.laboschqpa.filehost.enums.UploadedFileType;
import com.laboschqpa.filehost.exceptions.FileServingRateLimitHitException;
import com.laboschqpa.filehost.model.download.FileDownloadRequest;
import com.laboschqpa.filehost.model.upload.FileUploadRequest;
import com.laboschqpa.filehost.service.fileservingauth.AuthorizeRequestResult;
import com.laboschqpa.filehost.service.fileservingauth.FileServingUserAuthorizerService;
import com.laboschqpa.filehost.service.fileservingevent.FileServingEventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;

@Log4j2
@RequiredArgsConstructor
@RestController
@RequestMapping(AppConstants.userAccessibleBaseUrl + "/file")
public class FileServingController {
    private final FileServingUserAuthorizerService fileServingUserAuthorizerService;

    private final FileServingEventService fileServingEventService;
    private final FileDownloaderService fileDownloaderService;
    private final FileUploaderService fileUploaderService;

    @GetMapping("/get/**")
    public ResponseEntity<Resource> getDownload(@RequestParam("id") Long fileId,
                                                @RequestParam(value = "forceOriginal", required = false) String forceOriginalString,
                                                @RequestParam(value = "wantedImageSize", required = false) String wantedImageSizeString,
                                                HttpServletRequest httpServletRequest) {
        final Boolean forceOriginal = extractBooleanFromString(forceOriginalString);
        final Integer wantedImageSize = extractIntegerFromString(wantedImageSizeString);

        AuthorizeRequestResult authorizeRequestResult
                = fileServingUserAuthorizerService.authorizeRequestOrThrow(fileId, FileAccessType.READ, httpServletRequest);
        long requesterUserId = authorizeRequestResult.getLoggedInUserId();

        if (!fileServingEventService.isRateLimitAlright(requesterUserId)) {
            fileServingEventService.log(FileServingEventKind.DENIED_BECAUSE_OF_RATE_LIMIT, requesterUserId, fileId);
            throw new FileServingRateLimitHitException();
        }

        try {
            ResponseEntity<Resource> res = doDownload(fileId, forceOriginal, wantedImageSize, httpServletRequest);
            fileServingEventService.log(FileServingEventKind.SERVING_RESPONSE_CREATED_SUCCESSFULLY, requesterUserId, fileId);
            return res;
        } catch (Exception e) {
            fileServingEventService.log(FileServingEventKind.ERROR_WHILE_CREATING_SERVING_RESPONSE, requesterUserId, fileId);
            throw e;
        }
    }

    private ResponseEntity<Resource> doDownload(long fileId, @Nullable Boolean forceOriginal,
                                                @Nullable Integer wantedImageSize,
                                                HttpServletRequest httpServletRequest) {
        if (forceOriginal != null && forceOriginal) {
            return fileDownloaderService.downloadOriginalFile(fileId, httpServletRequest);
        }

        final FileDownloadRequest downloadRequest = new FileDownloadRequest(fileId, wantedImageSize);
        return fileDownloaderService.downloadOptimalFile(downloadRequest, httpServletRequest);
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
                = new FileUploadRequest(
                authorizeRequestReturn.getLoggedInUserId(),
                authorizeRequestReturn.getLoggedInUserTeamId(),
                UploadKind.BY_USER,
                forcedFileType);

        return fileUploaderService.uploadFile(fileUploadRequest, httpServletRequest);
    }

    private Boolean extractBooleanFromString(String str) {
        if (str == null) {
            return null;
        }
        final String trimmed = str.trim();

        if ("1".equals(trimmed) || "true".equals(trimmed)) {
            return true;
        }
        if ("0".equals(trimmed) || "false".equals(trimmed)) {
            return false;
        }
        return null;
    }

    private Integer extractIntegerFromString(String str) {
        if (str == null) {
            return null;
        }
        final String trimmed = str.trim();

        if (!StringUtils.isNumeric(trimmed)) {
            return null;
        }

        try {
            return Integer.parseInt(trimmed);
        } catch (Exception e) {
            return null;
        }
    }
}
