package com.laboschcst.filehost.service.apiclient;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.laboschcst.filehost.exceptions.ApiClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public abstract class AbstractApiClient {
    private static final Logger logger = LoggerFactory.getLogger(AbstractApiClient.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    protected abstract String getApiBaseUrl();

    protected abstract WebClient getWebClient();

    protected <T> T doCallAndThrowExceptionIfStatuscodeIsNot2xx(Class<T> responseBodyClass, String uriPath, HttpMethod httpMethod) {
        return doCallAndThrowExceptionIfStatuscodeIsNot2xx(responseBodyClass, uriPath, httpMethod, null, null);
    }

    protected <T> T doCallAndThrowExceptionIfStatuscodeIsNot2xx(Class<T> responseBodyClass, String uriPath, HttpMethod httpMethod, Map<String, String> queryParams) {
        return doCallAndThrowExceptionIfStatuscodeIsNot2xx(responseBodyClass, uriPath, httpMethod, queryParams, null);
    }

    protected <T> T doCallAndThrowExceptionIfStatuscodeIsNot2xx(Class<T> responseBodyClass, String uriPath, HttpMethod httpMethod, JsonNode requestBody) {
        return doCallAndThrowExceptionIfStatuscodeIsNot2xx(responseBodyClass, uriPath, httpMethod, null, requestBody);
    }

    protected <T> T doCallAndThrowExceptionIfStatuscodeIsNot2xx(Class<T> responseBodyClass, String uriPath, HttpMethod httpMethod, Map<String, String> queryParams, JsonNode requestBody) {
        HttpHeaders httpHeaders = new HttpHeaders();
        String requestBodyString = "";

        if (requestBody != null) {
            requestBodyString = requestBody.toString();
            httpHeaders.add("Content-Type", "application/json");
        }

        return doCallAndThrowExceptionIfStatuscodeIsNot2xx(responseBodyClass, uriPath, httpMethod, queryParams, requestBodyString, httpHeaders, null);
    }

    protected <T> T doCallAndThrowExceptionIfStatuscodeIsNot2xx(Class<T> responseBodyClass, String uriPath, HttpMethod httpMethod, Map<String, String> queryParams, String requestBody, HttpHeaders headers, MultiValueMap<String, String> cookies) {
        if (requestBody == null)
            requestBody = "";

        String queryString = "";
        if (queryParams != null) {
            queryString = queryParams.keySet().stream()
                    .map(key -> key + "=" + URLEncoder.encode(queryParams.get(key) != null ? queryParams.get(key) : "", StandardCharsets.UTF_8))
                    .collect(Collectors.joining("&"));
        }

        String fullUriString = getApiBaseUrl() + uriPath + "?" + queryString;

        ClientResponse response = getWebClient()
                .method(httpMethod)
                .uri(fullUriString)
                .cookies(requestCookies -> {
                    if (cookies != null)
                        requestCookies.addAll(cookies);
                })
                .body(BodyInserters.fromValue(requestBody))
                .headers(httpHeaders -> httpHeaders.addAll(headers))
                .exchange()
                .onErrorResume(e -> {
                    logger.error("Error during communication with {}", fullUriString);
                    return Mono.error(new ApiClientException(e));
                })
                .block();

        if (response.statusCode().is2xxSuccessful()) {
            try {
                T responseBody = response.bodyToMono(responseBodyClass).block();
                logger.debug("Rest call succeeded. Url: {{} {}}, HTTP status code: {{}}\nresponse:\n{}",
                        httpMethod,
                        fullUriString,
                        response.statusCode(),
                        responseBody
                );

                return responseBody;
            } catch (Exception e) {
                logger.debug("Exception while getting response body. Expected body class type: {}, Url: {{} {}}, HTTP status code: {{}}",
                        responseBodyClass.getSimpleName(),
                        httpMethod,
                        fullUriString,
                        response.statusCode()
                );

                throw e;
            }
        } else {
            logger.error("HTTP status code indicates failure. Url: {{} {}}, HTTP status code: {{}}\nresponse:\n{}",
                    httpMethod,
                    fullUriString,
                    response.statusCode(),
                    response.bodyToMono(String.class).block()
            );
            throw new ApiClientException("HTTP status code indicates failure", response.statusCode());
        }
    }
}
