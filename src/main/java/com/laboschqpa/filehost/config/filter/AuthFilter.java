package com.laboschqpa.filehost.config.filter;

import com.laboschqpa.filehost.api.dto.ExternalIndexedFileServingRequestDto;
import com.laboschqpa.filehost.api.errorhandling.ApiErrorResponseBody;
import com.laboschqpa.filehost.exceptions.apierrordescriptor.ApiErrorDescriptorException;
import com.laboschqpa.filehost.service.apiclient.qpaserver.dto.IsUserAuthorizedToResourceResponseDto;
import com.laboschqpa.filehost.enums.FileAccessType;
import com.laboschqpa.filehost.exceptions.InvalidHttpRequestException;
import com.laboschqpa.filehost.exceptions.UnAuthorizedException;
import com.laboschqpa.filehost.repo.IndexedFileEntityRepository;
import com.laboschqpa.filehost.repo.dto.IndexedFileOnlyJpaDto;
import com.laboschqpa.filehost.service.apiclient.qpaserver.dto.IsUserAuthorizedToResourceRequestDto;
import com.laboschqpa.filehost.service.apiclient.qpaserver.QpaServerApiClient;
import com.laboschqpa.filehost.util.ServletHelper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * <ul>
 * <li>Requests that are directly (from outside the cluster) reaching this service are served as file up/download.
 * They are auth-ed by calling the {@code Server} and wrapped into a {@link AuthWrappedHttpServletRequest}.</li>
 *
 * <li>Requests that are coming from the {@code Server} are served as normal API requests.
 * They are auth-ed by verifying the {@code AuthInterService} header value.</li>
 *
 * <li>If the {@code AuthInterService} header is present, the request is treated as it comes from the {@code Server}.</li>
 * </ul>
 * <p>
 * Only file up- and downloads (actions that require large data streaming via HTTP) should come directly from outside the cluster.
 */
@Component
@Order(2)
@RequiredArgsConstructor
public class AuthFilter implements Filter {
    private static final Logger logger = LoggerFactory.getLogger(AuthFilter.class);
    private static final String AUTH_INTER_SERVICE_HEADER_NAME = "AuthInterService";

    private boolean authSkipAll;

    private final QpaServerApiClient qpaServerApiClient;
    private final IndexedFileEntityRepository indexedFileEntityRepository;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest wrappedHttpServletRequest;

        try {
            wrappedHttpServletRequest = assertIfRequestProcessingCanBeContinued_andGetWrappedHttpServletRequest((HttpServletRequest) request);
        } catch (ApiErrorDescriptorException e) {
            ServletHelper.setJsonResponse((HttpServletResponse) response, new ApiErrorResponseBody(e.getApiErrorDescriptor(), e.getMessage()), HttpStatus.CONFLICT.value());
            wrappedHttpServletRequest = null;
        } catch (UnAuthorizedException e) {
            logger.trace("Request is unauthorized in AuthFilter: " + e.getMessage());
            ServletHelper.setJsonResponse((HttpServletResponse) response, new ApiErrorResponseBody("Unauthorized: " + e.getMessage()), HttpStatus.FORBIDDEN.value());
            wrappedHttpServletRequest = null;
        } catch (InvalidHttpRequestException e) {
            ServletHelper.setJsonResponse((HttpServletResponse) response, new ApiErrorResponseBody("Invalid request: " + e.getMessage()), HttpStatus.BAD_REQUEST.value());
            wrappedHttpServletRequest = null;
        } catch (Exception e) {
            logger.debug("Exception thrown while trying to authenticate incoming request!", e);
            ServletHelper.setJsonResponse((HttpServletResponse) response,
                    new ApiErrorResponseBody("Exception thrown while trying to authenticate incoming request!"), HttpStatus.FORBIDDEN.value());
            wrappedHttpServletRequest = null;
        }

        if (wrappedHttpServletRequest != null) {
            chain.doFilter(wrappedHttpServletRequest, response);
        }
    }

    private HttpServletRequest assertIfRequestProcessingCanBeContinued_andGetWrappedHttpServletRequest(HttpServletRequest httpServletRequest) {
        if (authSkipAll) {
            logger.trace("Skipping auth filter according to app configuration!");
            //Defaulting to normal API request
            return httpServletRequest;
        } else {
            return authorizeRequestAndGetWrappedHttpServletRequest(httpServletRequest);
        }
    }

    private HttpServletRequest authorizeRequestAndGetWrappedHttpServletRequest(HttpServletRequest httpServletRequest) {
        final String authInterServiceHeader = httpServletRequest.getHeader(AUTH_INTER_SERVICE_HEADER_NAME);

        final AuthMethod authMethod;
        if (authInterServiceHeader != null && !authInterServiceHeader.isBlank()) {
            authMethod = AuthMethod.AUTH_INTER_SERVICE;
        } else {
            authMethod = AuthMethod.USER_SESSION;
        }

        switch (authMethod) {
            case AUTH_INTER_SERVICE: {
                if (isAuthInterServiceHeaderValid(authInterServiceHeader)) {
                    logger.trace("AuthFilter auth is valid for call between services.");
                    return new AuthWrappedHttpServletRequest(httpServletRequest, authMethod, null);
                } else {
                    throw new UnAuthorizedException("AuthInterService header is invalid.");
                }
            }
            case USER_SESSION: {
                return new AuthWrappedHttpServletRequest(httpServletRequest, authMethod, authorizeExternalCall(httpServletRequest));
            }
            default: {
                throw new IllegalStateException("Invalid AUthMethod: " + authMethod);
            }
        }
    }

    private WrappedExternalFileServingRequestDto authorizeExternalCall(HttpServletRequest httpServletRequest) {
        final ExternalIndexedFileServingRequestDto externalRequestDto
                = ExternalIndexedFileServingRequestDto.Factory.createFrom((httpServletRequest));

        final IsUserAuthorizedToResourceRequestDto isUserAuthorizedToResourceRequestDto = IsUserAuthorizedToResourceRequestDto.builder()
                .httpMethod(externalRequestDto.getHttpMethod())
                .csrfToken(externalRequestDto.getCsrfToken())
                .indexedFileId(externalRequestDto.getIndexedFileId())
                .fileAccessType(externalRequestDto.getFileAccessType())
                .build();

        if (externalRequestDto.getFileAccessType() == FileAccessType.READ) {
            final IndexedFileOnlyJpaDto indexedFileOnlyJpaDto = indexedFileEntityRepository.getValidExistingAvailableIndexedFileOnlyJpaDto(externalRequestDto.getIndexedFileId());
            isUserAuthorizedToResourceRequestDto.setIndexedFileOwnerUserId(indexedFileOnlyJpaDto.getOwnerUserId());
            isUserAuthorizedToResourceRequestDto.setIndexedFileOwnerTeamId(indexedFileOnlyJpaDto.getOwnerTeamId());
        }

        final Cookie sessionCookie = extractSessionCookie(httpServletRequest);
        final IsUserAuthorizedToResourceResponseDto isAuthorizedResponseDto
                = qpaServerApiClient.getIsAuthorizedToResource(sessionCookie.getValue(), isUserAuthorizedToResourceRequestDto);

        assertIsAuthorizedToResourceResponseIsPositive(isAuthorizedResponseDto, externalRequestDto);

        logger.trace("AuthFilter auth is valid for external call.");
        return WrappedExternalFileServingRequestDto.builder()
                .fileAccessType(externalRequestDto.getFileAccessType())
                .indexedFileId(externalRequestDto.getIndexedFileId())
                .loggedInUserId(isAuthorizedResponseDto.getLoggedInUserId())
                .loggedInUserTeamId(isAuthorizedResponseDto.getLoggedInUserTeamId())
                .build();
    }

    private void assertIsAuthorizedToResourceResponseIsPositive(IsUserAuthorizedToResourceResponseDto isAuthorizedResponseDto,
                                                                ExternalIndexedFileServingRequestDto externalIndexedFileServingRequestDto) {
        if (!isAuthorizedResponseDto.isAuthenticated())
            throw new UnAuthorizedException("Cannot authenticate user of the session. You are probably not logged in.");

        if (!isAuthorizedResponseDto.isCsrfValid())
            throw new UnAuthorizedException("CSRF token is invalid.");

        if (!isAuthorizedResponseDto.isAuthorized())
            throw new UnAuthorizedException("User of the sent Session is unauthorized for the requested resource: " + externalIndexedFileServingRequestDto.getIndexedFileId());

        if (isAuthorizedResponseDto.getLoggedInUserId() == null || isAuthorizedResponseDto.getLoggedInUserId() < 1) {
            logger.error("loggedInUserId ({}) is invalid in response got from Server!", isAuthorizedResponseDto.getLoggedInUserId());
            throw new RuntimeException("loggedInUserId is invalid in response got from Server!");
        }

        if (externalIndexedFileServingRequestDto.getFileAccessType() == FileAccessType.CREATE_NEW) {
            if (isAuthorizedResponseDto.getLoggedInUserTeamId() == null || isAuthorizedResponseDto.getLoggedInUserTeamId() < 1) {
                logger.error("loggedInUserTeamId ({}) is invalid in response got from Server!", isAuthorizedResponseDto.getLoggedInUserTeamId());
                throw new RuntimeException("loggedInUserTeamId is invalid in response got from Server!");
            }
        }
    }

    private Cookie extractSessionCookie(HttpServletRequest httpServletRequest) {
        Cookie[] cookies = httpServletRequest.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("SESSION".equals(cookie.getName())) {
                    return cookie;
                }
            }
        }
        throw new InvalidHttpRequestException("Cannot found session cookie! You are probably not logged in.");
    }

    private boolean isAuthInterServiceHeaderValid(String authHeader) {
        return authHeader.equals(System.getProperty("auth.interservice.key"));
    }

    @Value("${authfilter.skip.all:false}")
    public void setAuthSkipAll(Boolean authSkipAll) {
        this.authSkipAll = authSkipAll;
    }
}