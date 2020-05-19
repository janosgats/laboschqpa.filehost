package com.laboschqpa.filehost.config.filter;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.laboschqpa.filehost.api.dto.IndexedFileServingRequestDto;
import com.laboschqpa.filehost.api.dto.IsUserAuthorizedToResourceResponseDto;
import com.laboschqpa.filehost.enums.FileAccessType;
import com.laboschqpa.filehost.enums.IndexedFileStatus;
import com.laboschqpa.filehost.exceptions.ContentNotFoundApiException;
import com.laboschqpa.filehost.exceptions.InvalidHttpRequestException;
import com.laboschqpa.filehost.exceptions.UnAuthorizedException;
import com.laboschqpa.filehost.exceptions.fileserving.FileIsNotAvailableException;
import com.laboschqpa.filehost.repo.IndexedFileEntityRepository;
import com.laboschqpa.filehost.repo.dto.IndexedFileOnlyJpaDto;
import com.laboschqpa.filehost.service.apiclient.qpaserver.GetIsUserAuthorizedToResourceDto;
import com.laboschqpa.filehost.service.apiclient.qpaserver.QpaServerApiClient;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;

/**
 * <ul>
 * <li>Requests that are directly reaching this service are served as file up/download/delete.
 * They are auth-ed by calling the {@code Server} and wrapped into a {@link WrappedFileServingHttpServletRequest}.</li>
 *
 * <li>Requests that are coming from the {@code Server} are served as normal API requests.
 * They are auth-ed by verifying the {@code AuthInterService} header value.</li>
 *
 * <li>If the {@code AuthInterService} header is present, the request is treated as it comes from the {@code Server}.</li>
 * </ul>
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
        } catch (FileIsNotAvailableException e) {
            logger.trace("FileIsNotAvailableException in AuthFilter: " + e.getMessage());
            writeErrorResponseBody((HttpServletResponse) response, "Resource is not available: " + e.getMessage(), HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE);
            wrappedHttpServletRequest = null;
        } catch (ContentNotFoundApiException e) {
            logger.trace("ContentNotFoundApiException in AuthFilter: " + e.getMessage());
            writeErrorResponseBody((HttpServletResponse) response, "Resource not found: " + e.getMessage(), HttpStatus.NOT_FOUND);
            wrappedHttpServletRequest = null;
        } catch (UnAuthorizedException e) {
            logger.trace("Request is unauthorized in AuthFilter: " + e.getMessage());
            writeErrorResponseBody((HttpServletResponse) response, "Unauthorized: " + e.getMessage(), HttpStatus.FORBIDDEN);
            wrappedHttpServletRequest = null;
        } catch (InvalidHttpRequestException e) {
            writeErrorResponseBody((HttpServletResponse) response, "Invalid request: " + e.getMessage(), HttpStatus.BAD_REQUEST);
            wrappedHttpServletRequest = null;
        } catch (Exception e) {
            logger.debug("Exception thrown while trying to authenticate incoming request!", e);
            writeErrorResponseBody((HttpServletResponse) response, "Exception thrown while trying to authenticate incoming request!", HttpStatus.INTERNAL_SERVER_ERROR);
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
        String authInterServiceHeader = httpServletRequest.getHeader(AUTH_INTER_SERVICE_HEADER_NAME);
        if (authInterServiceHeader != null && !authInterServiceHeader.isBlank()) {
            //Authorizing call between services
            if (isAuthInterServiceHeaderValid(authInterServiceHeader)) {
                logger.trace("AuthFilter auth is valid for call between services.");
                return httpServletRequest;
            } else {
                throw new UnAuthorizedException("AuthInterService header is invalid.");
            }
        } else {
            //Authorizing external call
            WrappedFileServingRequestDto wrappedFileServingRequestDto = authorizeCallFromAUser(httpServletRequest);
            return new WrappedFileServingHttpServletRequest(httpServletRequest, wrappedFileServingRequestDto);
        }
    }

    private WrappedFileServingRequestDto authorizeCallFromAUser(HttpServletRequest httpServletRequest) {
        final IndexedFileServingRequestDto indexedFileServingRequestDto = getIndexedFileServingRequestDtoFromRequest(httpServletRequest);

        final GetIsUserAuthorizedToResourceDto getIsUserAuthorizedToResourceDto = GetIsUserAuthorizedToResourceDto.builder()
                .httpMethod(indexedFileServingRequestDto.getHttpMethod())
                .csrfToken(indexedFileServingRequestDto.getCsrfToken())
                .indexedFileId(indexedFileServingRequestDto.getIndexedFileId())
                .fileAccessType(indexedFileServingRequestDto.getFileAccessType())
                .build();

        if (indexedFileServingRequestDto.getFileAccessType() == FileAccessType.READ
                || indexedFileServingRequestDto.getFileAccessType() == FileAccessType.DELETE) {
            final IndexedFileOnlyJpaDto indexedFileOnlyJpaDto = getValidExistingAvailableIndexedFileOnlyJpaDto(indexedFileServingRequestDto);
            getIsUserAuthorizedToResourceDto.setIndexedFileOwnerUserId(indexedFileOnlyJpaDto.getOwnerUserId());
            getIsUserAuthorizedToResourceDto.setIndexedFileOwnerTeamId(indexedFileOnlyJpaDto.getOwnerTeamId());
        }

        final Cookie sessionCookie = getSessionCookie(httpServletRequest);
        final IsUserAuthorizedToResourceResponseDto isAuthorizedResponseDto
                = qpaServerApiClient.getIsAuthorizedToResource(sessionCookie.getValue(), getIsUserAuthorizedToResourceDto);

        assertIsAuthorizedToResourceResponseIsPositive(isAuthorizedResponseDto, indexedFileServingRequestDto);

        logger.trace("AuthFilter auth is valid for external call.");
        return WrappedFileServingRequestDto.builder()
                .fileAccessType(indexedFileServingRequestDto.getFileAccessType())
                .indexedFileId(indexedFileServingRequestDto.getIndexedFileId())
                .loggedInUserId(isAuthorizedResponseDto.getLoggedInUserId())
                .loggedInUserTeamId(isAuthorizedResponseDto.getLoggedInUserTeamId())
                .build();
    }

    private void assertIsAuthorizedToResourceResponseIsPositive(IsUserAuthorizedToResourceResponseDto isAuthorizedResponseDto,
                                                                IndexedFileServingRequestDto indexedFileServingRequestDto) {
        if (!isAuthorizedResponseDto.isAuthenticated())
            throw new UnAuthorizedException("Cannot authenticate user of the session. You are probably not logged in.");

        if (!isAuthorizedResponseDto.isCsrfValid())
            throw new UnAuthorizedException("CSRF token is invalid.");

        if (!isAuthorizedResponseDto.isAuthorized())
            throw new UnAuthorizedException("User of the sent Session is unauthorized for the requested resource: " + indexedFileServingRequestDto.getIndexedFileId());

        if (isAuthorizedResponseDto.getLoggedInUserId() == null || isAuthorizedResponseDto.getLoggedInUserId() < 1) {
            logger.error("loggedInUserId ({}) is invalid in response got from Server!", isAuthorizedResponseDto.getLoggedInUserId());
            throw new RuntimeException("loggedInUserId is invalid in response got from Server!");
        }

        if (isAuthorizedResponseDto.getLoggedInUserTeamId() == null || isAuthorizedResponseDto.getLoggedInUserTeamId() < 1) {
            logger.error("loggedInUserTeamId ({}) is invalid in response got from Server!", isAuthorizedResponseDto.getLoggedInUserTeamId());
            throw new RuntimeException("loggedInUserTeamId is invalid in response got from Server!");
        }
    }

    private IndexedFileOnlyJpaDto getValidExistingAvailableIndexedFileOnlyJpaDto(IndexedFileServingRequestDto indexedFileServingRequestDto) {
        Optional<IndexedFileOnlyJpaDto> indexedFileOnlyJpaDtoOptional
                = indexedFileEntityRepository.findOnlyFromIndexedFileTableById(indexedFileServingRequestDto.getIndexedFileId());

        if (indexedFileOnlyJpaDtoOptional.isEmpty()) {
            throw new ContentNotFoundApiException("File with id "
                    + indexedFileServingRequestDto.getIndexedFileId() + " does not exist!");
        }
        IndexedFileOnlyJpaDto indexedFileOnlyJpaDto = indexedFileOnlyJpaDtoOptional.get();
        if (indexedFileOnlyJpaDto.getStatus() != IndexedFileStatus.AVAILABLE) {
            throw new FileIsNotAvailableException("File with id "
                    + indexedFileServingRequestDto.getIndexedFileId() + " is found, but it's not available!");
        }
        return indexedFileOnlyJpaDto;
    }

    private IndexedFileServingRequestDto getIndexedFileServingRequestDtoFromRequest(HttpServletRequest httpServletRequest) {
        HttpMethod httpMethod = HttpMethod.resolve(httpServletRequest.getMethod());
        if (httpMethod == null)
            throw new InvalidHttpRequestException("No valid HttpMethod is specified! (" + httpServletRequest.getMethod() + ")");
        String csrfToken = httpServletRequest.getHeader("X-CSRF-TOKEN");

        FileAccessType fileAccessType = getRequestedFileAccessType(httpServletRequest);

        Long requestedIndexedFileId = null;
        if (fileAccessType == FileAccessType.READ || fileAccessType == FileAccessType.DELETE)
            requestedIndexedFileId = getRequestedIndexedFileId(httpServletRequest);

        return IndexedFileServingRequestDto
                .builder()
                .httpMethod(httpMethod)
                .csrfToken(csrfToken)
                .indexedFileId(requestedIndexedFileId)
                .fileAccessType(fileAccessType)
                .build();
    }

    private Long getRequestedIndexedFileId(HttpServletRequest httpServletRequest) {
        HashSet<String> indexedFileIdParamValues = getHashSetOfParamValues(httpServletRequest, "id", "indexedFileId");

        if (indexedFileIdParamValues.size() > 1)
            throw new InvalidHttpRequestException("indexedFileId is specified multiple times with different values.");
        else if (indexedFileIdParamValues.size() == 0)
            throw new InvalidHttpRequestException("No indexedFileId parameter is present.");

        return Long.parseLong(indexedFileIdParamValues.iterator().next());
    }

    private HashSet<String> getHashSetOfParamValues(HttpServletRequest httpServletRequest, String... paramAliases) {
        final HashSet<String> values = new HashSet<>();
        for (String name : paramAliases) {
            final String[] buffParams = httpServletRequest.getParameterValues(name);
            if (buffParams != null)
                values.addAll(Arrays.asList(buffParams));
        }
        return values;
    }

    private FileAccessType getRequestedFileAccessType(HttpServletRequest httpServletRequest) {
        HttpMethod httpMethod = HttpMethod.resolve(httpServletRequest.getMethod());
        if (httpMethod == null)
            throw new InvalidHttpRequestException("No valid HttpMethod is specified! (" + httpServletRequest.getMethod() + ")");

        switch (httpMethod) {
            case GET:
                return FileAccessType.READ;
            case DELETE:
                return FileAccessType.DELETE;
            case POST:
                return FileAccessType.WRITE;
            default:
                throw new InvalidHttpRequestException("Cannot map given HttpMethod to FileAccessType: " + httpMethod);
        }
    }

    private Cookie getSessionCookie(HttpServletRequest httpServletRequest) {
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

    private void writeErrorResponseBody(HttpServletResponse httpServletResponse, String errorMessage, HttpStatus
            httpStatus) throws IOException {
        ObjectNode responseObjectNode = new ObjectNode(JsonNodeFactory.instance);
        responseObjectNode.put("error", errorMessage);
        String responseBody = responseObjectNode.toString();

        httpServletResponse.setContentType("application/json");
        httpServletResponse.setContentLength(responseBody.length());
        httpServletResponse.getWriter().write(responseBody);
        httpServletResponse.setStatus(httpStatus.value());
    }

    @Value("${authfilter.skip.all:false}")
    public void setAuthSkipAll(Boolean authSkipAll) {
        this.authSkipAll = authSkipAll;
    }
}