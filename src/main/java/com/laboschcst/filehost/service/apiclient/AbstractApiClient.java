package com.laboschcst.filehost.service.apiclient;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.laboschcst.filehost.exceptions.ApiClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    protected String doCallAndThrowExceptionIfStatuscodeIsNot2xx(String uriPath, HttpMethod httpMethod) {
        return doCallAndThrowExceptionIfStatuscodeIsNot2xx(uriPath, httpMethod, null, null, null);
    }

    protected String doCallAndThrowExceptionIfStatuscodeIsNot2xx(String uriPath, HttpMethod httpMethod, Map<String, String> queryParams) {
        return doCallAndThrowExceptionIfStatuscodeIsNot2xx(uriPath, httpMethod, queryParams, null, null);
    }

    protected String doCallAndThrowExceptionIfStatuscodeIsNot2xx(String uriPath, HttpMethod httpMethod, JsonNode requestBody) {
        return doCallAndThrowExceptionIfStatuscodeIsNot2xx(uriPath, httpMethod, null, requestBody, null);
    }

    protected String doCallAndThrowExceptionIfStatuscodeIsNot2xx(String uriPath, HttpMethod httpMethod, Map<String, String> queryParams, JsonNode requestBody) {
        return doCallAndThrowExceptionIfStatuscodeIsNot2xx(uriPath, httpMethod, null, requestBody, null);
    }

    protected String doCallAndThrowExceptionIfStatuscodeIsNot2xx(String uriPath, HttpMethod httpMethod, Map<String, String> queryParams, JsonNode requestBody, MultiValueMap<String, String> cookies) {
        String queryString = "";
        if (queryParams != null) {
            queryString = queryParams.keySet().stream()
                    .map(key -> key + "=" + URLEncoder.encode(queryParams.get(key) != null ? queryParams.get(key) : "", StandardCharsets.UTF_8))
                    .collect(Collectors.joining("&"));
        }

        String fullUri = getApiBaseUrl() + uriPath + "?" + queryString;

        HashMap<String, String> headers = new HashMap<>();
        String requestBodyString = "";
        if (requestBody != null) {
            requestBodyString = requestBody.toString();
            headers.put("Content-Type", "application/json");
        }

        ClientResponse response = getWebClient()
                .method(httpMethod)
                .uri(fullUri)
                .cookies(requestCookies -> {
                    if (cookies != null)
                        requestCookies.addAll(cookies);
                })
                .body(BodyInserters.fromValue(requestBodyString))
                .header("Content-Type", headers.get("Content-Type"))
                .exchange()
                .onErrorResume(e -> {
                    logger.error("Error during communication with {}", fullUri);
                    return Mono.error(new ApiClientException(e));
                })
                .block();

        if (response.statusCode().is2xxSuccessful()) {
            String responseBody = response.bodyToMono(String.class).block();
            logger.debug("Rest call succeeded. Url: {{} {}}, HTTP status code: {{}}\nresponse:\n{}",
                    httpMethod,
                    fullUri,
                    response.statusCode(),
                    responseBody
            );
            return responseBody;
        } else {
            logger.error("HTTP status code from performer.webservice indicates failure. Url: {{} {}}, HTTP status code: {{}}\nresponse:\n{}",
                    httpMethod,
                    fullUri,
                    response.statusCode(),
                    response.bodyToMono(String.class).block()
            );
            throw new ApiClientException("HTTP status code from performer.webservice indicates failure", response.statusCode());
        }
    }

    public static JsonNode convertStringToJsonNode(String str) {
        try {
            return objectMapper.readTree(str);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Cannot convert string to JsonNode.", e);
        }
    }
}
