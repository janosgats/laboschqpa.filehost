package com.laboschqpa.filehost.service.fileservingauth;

import com.laboschqpa.filehost.enums.FileAccessType;
import com.laboschqpa.filehost.exceptions.InvalidHttpRequestException;
import com.laboschqpa.filehost.exceptions.UnAuthorizedException;
import com.laboschqpa.filehost.repo.IndexedFileEntityRepository;
import com.laboschqpa.filehost.repo.dto.IndexedFileOnlyJpaDto;
import com.laboschqpa.filehost.service.apiclient.qpaserver.QpaServerApiClient;
import com.laboschqpa.filehost.service.apiclient.qpaserver.dto.IsUserAuthorizedToResourceRequestDto;
import com.laboschqpa.filehost.service.apiclient.qpaserver.dto.IsUserAuthorizedToResourceResponseDto;
import com.laboschqpa.filehost.util.CookieHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;

@Log4j2
@RequiredArgsConstructor
@Service
public class FileServingUserAuthorizerService {
    private static final String HEADER_NAME_CSRF_TOKEN = "X-CSRF-TOKEN";

    private final IndexedFileEntityRepository indexedFileEntityRepository;
    private final QpaServerApiClient qpaServerApiClient;

    public AuthorizeRequestResult authorizeRequestOrThrow(Long fileId, FileAccessType fileAccessType,
                                                          HttpServletRequest httpServletRequest) {
        final IsUserAuthorizedToResourceRequestDto isUserAuthorizedToResourceRequestDto
                = createIsUserAuthorizedToResourceRequestDto(fileId, fileAccessType, httpServletRequest);

        final IsUserAuthorizedToResourceResponseDto isAuthorizedResponseDto
                = qpaServerApiClient.getIsUserAuthorizedToResource(isUserAuthorizedToResourceRequestDto);

        assertIsAuthorizedToResourceResponseIsPositive(isAuthorizedResponseDto, fileId, fileAccessType);
        return new AuthorizeRequestResult(isAuthorizedResponseDto.getLoggedInUserId(), isAuthorizedResponseDto.getLoggedInUserTeamId());
    }

    IsUserAuthorizedToResourceRequestDto createIsUserAuthorizedToResourceRequestDto(Long fileId, FileAccessType fileAccessType,
                                                                                    HttpServletRequest httpServletRequest) {
        final String sessionId = CookieHelper.extractUnencodedSessionCookieOrThrow(httpServletRequest);
        if (StringUtils.isBlank(sessionId)) {
            throw new InvalidHttpRequestException("No Session ID was found in the request");
        }

        final IsUserAuthorizedToResourceRequestDto isUserAuthorizedToResourceRequestDto = IsUserAuthorizedToResourceRequestDto.builder()
                .sessionId(sessionId)
                .csrfToken(httpServletRequest.getHeader(HEADER_NAME_CSRF_TOKEN))
                .indexedFileId(fileId)
                .fileAccessType(fileAccessType)
                .build();

        if (fileAccessType == FileAccessType.READ) {
            final IndexedFileOnlyJpaDto indexedFileOnlyJpaDto = indexedFileEntityRepository.getValidExistingAvailableIndexedFileOnlyJpaDto(fileId);
            isUserAuthorizedToResourceRequestDto.setIndexedFileOwnerUserId(indexedFileOnlyJpaDto.getOwnerUserId());
            isUserAuthorizedToResourceRequestDto.setIndexedFileOwnerTeamId(indexedFileOnlyJpaDto.getOwnerTeamId());
        }

        return isUserAuthorizedToResourceRequestDto;
    }

    void assertIsAuthorizedToResourceResponseIsPositive(IsUserAuthorizedToResourceResponseDto isAuthorizedResponseDto,
                                                        Long fileId, FileAccessType fileAccessType) {
        if (!isAuthorizedResponseDto.isAuthenticated())
            throw new UnAuthorizedException("Cannot authenticate user of the session. You are probably not logged in.");

        if (!isAuthorizedResponseDto.isCsrfValid())
            throw new UnAuthorizedException("CSRF token is invalid.");

        if (!isAuthorizedResponseDto.isAuthorized())
            throw new UnAuthorizedException("User of the sent Session is unauthorized for the requested resource: " + fileAccessType + " " + fileId);

        if (isAuthorizedResponseDto.getLoggedInUserId() == null || isAuthorizedResponseDto.getLoggedInUserId() < 1) {
            log.error("loggedInUserId ({}) is invalid in response got from Server!", isAuthorizedResponseDto.getLoggedInUserId());
            throw new RuntimeException("loggedInUserId is invalid in response got from Server!");
        }

        if (fileAccessType == FileAccessType.CREATE_NEW) {
            if (isAuthorizedResponseDto.getLoggedInUserTeamId() == null || isAuthorizedResponseDto.getLoggedInUserTeamId() < 1) {
                log.error("loggedInUserTeamId ({}) is invalid in response got from Server!", isAuthorizedResponseDto.getLoggedInUserTeamId());
                throw new RuntimeException("loggedInUserTeamId is invalid in response got from Server!");
            }
        }
    }
}
