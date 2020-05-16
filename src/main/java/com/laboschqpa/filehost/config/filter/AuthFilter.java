package com.laboschqpa.filehost.config.filter;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.laboschqpa.filehost.api.dto.IndexedFileServingRequestDto;
import com.laboschqpa.filehost.api.dto.IsUserAuthorizedToResourceResponseDto;
import com.laboschqpa.filehost.enums.FileAccessType;
import com.laboschqpa.filehost.exceptions.InvalidHttpRequestException;
import com.laboschqpa.filehost.exceptions.UnAuthorizedException;
import com.laboschqpa.filehost.service.apiclient.QpaServerApiClient;
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

@Component
@Order(2)
@RequiredArgsConstructor
public class AuthFilter implements Filter {
    private static final Logger logger = LoggerFactory.getLogger(AuthFilter.class);

    private boolean authSkipAll;

    private final QpaServerApiClient qpaServerApiClient;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpServletRequest = (HttpServletRequest) request;

        if (authSkipAll) {
            logger.trace("Skipping auth filter according to application.properties!");
            chain.doFilter(new FileServingHttpServletRequest(httpServletRequest, getIndexedFileResourceRequestDtoFromRequest(httpServletRequest)), response);
        } else {
            try {
                String authInterServiceHeader = httpServletRequest.getHeader("AuthInterService");
                if (authInterServiceHeader != null && !authInterServiceHeader.isBlank()) {
                    //Authorizing call between services
                    if (!isAuthInterServiceHeaderValid(authInterServiceHeader))
                        throw new UnAuthorizedException("AuthInterService header is invalid.");

                    logger.trace("AuthFilter auth is valid for call between services.");
                    chain.doFilter(request, response);
                } else {
                    //Authorizing call from a user
                    Cookie sessionCookie = getSessionCookie(httpServletRequest);

                    IndexedFileServingRequestDto indexedFileServingRequestDto = getIndexedFileResourceRequestDtoFromRequest(httpServletRequest);
                    IsUserAuthorizedToResourceResponseDto responseDto = qpaServerApiClient.getIsAuthorizedToResource(sessionCookie.getValue(), indexedFileServingRequestDto);

                    if (!responseDto.isAuthenticated())
                        throw new UnAuthorizedException("Cannot authenticate user of the session. You are probably not logged in.");

                    if (!responseDto.isCsrfValid())
                        throw new UnAuthorizedException("CSRF token is invalid.");

                    if (!responseDto.isAuthorized())
                        throw new UnAuthorizedException("User of the sent Session is unauthorized for the requested resource: " + indexedFileServingRequestDto.getIndexedFileId());

                    logger.trace("AuthFilter auth is valid for call from a user.");
                    chain.doFilter(new FileServingHttpServletRequest(httpServletRequest, indexedFileServingRequestDto), response);
                }

            } catch (UnAuthorizedException e) {
                logger.trace("Request is unauthorized in AuthFilter: " + e.getMessage());
                writeErrorResponseBody((HttpServletResponse) response, "Unauthorized: " + e.getMessage(), HttpStatus.FORBIDDEN);
            } catch (InvalidHttpRequestException e) {
                writeErrorResponseBody((HttpServletResponse) response, "Invalid request: " + e.getMessage(), HttpStatus.BAD_REQUEST);
            } catch (Exception e) {
                logger.debug("Exception thrown while trying to authenticate incoming request!", e);
                writeErrorResponseBody((HttpServletResponse) response, "Exception thrown while trying to authenticate incoming request!", HttpStatus.INTERNAL_SERVER_ERROR);
            }

        }
    }

    private IndexedFileServingRequestDto getIndexedFileResourceRequestDtoFromRequest(HttpServletRequest httpServletRequest) {
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
        String[] resourceIdParams = httpServletRequest.getParameterValues("indexedFileId");
        if (resourceIdParams == null)
            throw new InvalidHttpRequestException("No indexedFileId parameter is present.");

        if (resourceIdParams.length > 1)
            throw new InvalidHttpRequestException("Multi value is not allowed for indexedFileId parameter.");
        else if (resourceIdParams.length == 0)
            throw new InvalidHttpRequestException("No indexedFileId parameter is present.");

        return Long.parseLong(resourceIdParams[0]);
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
                return FileAccessType.UPLOAD;
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

    private void writeErrorResponseBody(HttpServletResponse httpServletResponse, String errorMessage, HttpStatus httpStatus) throws IOException {
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