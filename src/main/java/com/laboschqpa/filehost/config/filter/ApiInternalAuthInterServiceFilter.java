package com.laboschqpa.filehost.config.filter;

import com.laboschqpa.filehost.api.errorhandling.ApiErrorResponseBody;
import com.laboschqpa.filehost.config.AppConstants;
import com.laboschqpa.filehost.exceptions.UnAuthorizedException;
import com.laboschqpa.filehost.service.authinterservice.AuthInterServiceCrypto;
import com.laboschqpa.filehost.util.ServletHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;

@Log4j2
@Component
@Order(1)
@RequiredArgsConstructor
public class ApiInternalAuthInterServiceFilter implements Filter {
    public static final String HEADER_NAME_AUTH_INTER_SERVICE = "AuthInterService";
    private static final AntPathMatcher antPathMatcher = new AntPathMatcher();

    private final AuthInterServiceCrypto authInterServiceCrypto;

    @Value("${auth.interservice.logRequestAndResponseHeaders:false}")
    private Boolean logRequestAndResponseHeaders;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpServletRequest = (HttpServletRequest) request;

        logRequestHeaders(httpServletRequest);

        boolean canRequestProcessingBeContinued = false;
        try {
            canRequestProcessingBeContinued = decideIfRequestProcessingCanBeContinued(httpServletRequest, response, chain);
        } catch (UnAuthorizedException e) {
            writeErrorResponseBody((HttpServletResponse) response, "Unauthorized: " + e.getMessage(), HttpStatus.FORBIDDEN);
        } catch (Exception e) {
            log.debug("Exception thrown while trying to authenticate incoming request!", e);
            writeErrorResponseBody((HttpServletResponse) response, "Exception thrown while trying to authenticate incoming request!", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        try {
            if (canRequestProcessingBeContinued) {
                chain.doFilter(request, response);
            }
        } finally {
            logResponseHeaders((HttpServletResponse) response);
        }
    }

    private boolean decideIfRequestProcessingCanBeContinued(HttpServletRequest httpServletRequest, ServletResponse response, FilterChain chain) {
        final String requestUri = httpServletRequest.getRequestURI();
        if (shouldUrlBeSkipped(requestUri)) {
            log.trace("AuthInterService auth not required. URL: {}", httpServletRequest.getRequestURI());
            return true;
        }

        final String authInterServiceHeader = httpServletRequest.getHeader(HEADER_NAME_AUTH_INTER_SERVICE);
        if (authInterServiceCrypto.isHeaderValid(authInterServiceHeader)) {
            log.trace("AuthInterService auth passed. URL: {}", httpServletRequest.getRequestURI());
            return true;
        }

        log.trace("AuthInterService auth failed. URL: {}", httpServletRequest.getRequestURI());
        throw new UnAuthorizedException("AuthInterService header is invalid.");
    }

    private boolean shouldUrlBeSkipped(String requestUri) {
        return antPathMatcher.match(AppConstants.userAccessibleBaseUrlAntPattern, requestUri)
                || antPathMatcher.match(AppConstants.prometheusMetricsExposeUrl, requestUri)
                || antPathMatcher.match(AppConstants.healthPingUrlAntPattern, requestUri);
    }

    private void writeErrorResponseBody(HttpServletResponse httpServletResponse, String errorMessage, HttpStatus httpStatus) {
        ServletHelper.setJsonResponse(httpServletResponse, new ApiErrorResponseBody(errorMessage), httpStatus.value());
    }

    /**
     * Use this only for development to avoid logging sensitive information!
     */
    void logRequestHeaders(HttpServletRequest httpServletRequest) {
        if (!logRequestAndResponseHeaders)
            return;

        final List<String> headers = new ArrayList<>();
        setFromEnumeration(httpServletRequest.getHeaderNames()).forEach(headerName -> {
                    httpServletRequest.getHeaders(headerName).asIterator().forEachRemaining(headerValue -> {
                        headers.add(headerName + ": " + headerValue);
                    });
                }
        );

        log.debug("Request headers: ({} pcs)\n{}",
                headers.size(), Strings.join(headers, '\n'));

    }

    <T> Set<T> setFromEnumeration(Enumeration<T> enumeration) {
        final Set<T> set = new HashSet<>();
        enumeration.asIterator().forEachRemaining(set::add);
        return set;
    }

    /**
     * Use this only for development to avoid logging sensitive information!
     */
    void logResponseHeaders(HttpServletResponse httpServletResponse) {
        if (!logRequestAndResponseHeaders)
            return;

        final List<String> headers = new ArrayList<>();
        new HashSet<>(httpServletResponse.getHeaderNames()).forEach(headerName -> {
                    httpServletResponse.getHeaders(headerName).forEach(headerValue -> {
                        headers.add(headerName + ": " + headerValue);
                    });
                }
        );

        log.debug("Response headers: ({} pcs)\n{}",
                headers.size(), Strings.join(headers, '\n'));

    }
}
