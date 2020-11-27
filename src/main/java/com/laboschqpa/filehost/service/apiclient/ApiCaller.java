package com.laboschqpa.filehost.service.apiclient;

import com.fasterxml.jackson.databind.JsonNode;
import com.laboschqpa.filehost.exceptions.apiclient.ResponseCodeIsNotSuccessApiClientException;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ReactiveHttpOutputMessage;
import org.springframework.http.client.reactive.ClientHttpRequest;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserter;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.stream.Collectors;

@Log4j2
public class ApiCaller {
    private String apiBaseUrl;
    private WebClient webClient;
    private String[] secretsToHideInLogs;

    public ApiCaller(String apiBaseUrl, WebClient webClient, String[] secretsToHideInLogs) {

        this.apiBaseUrl = apiBaseUrl;
        this.webClient = webClient;
        this.secretsToHideInLogs = secretsToHideInLogs;
    }

    public <T> T doCallAndThrowExceptionIfStatuscodeIsNot2xx(final Class<T> responseBodyClass, String uriPath, HttpMethod httpMethod) {
        return doCallAndThrowExceptionIfStatuscodeIsNot2xx(responseBodyClass, uriPath, httpMethod, null, null);
    }

    public <T> T doCallAndThrowExceptionIfStatuscodeIsNot2xx(final Class<T> responseBodyClass, String uriPath, HttpMethod httpMethod, Map<String, String> queryParams) {
        return doCallAndThrowExceptionIfStatuscodeIsNot2xx(responseBodyClass, uriPath, httpMethod, queryParams, null);
    }

    public <T> T doCallAndThrowExceptionIfStatuscodeIsNot2xx(final Class<T> responseBodyClass, String uriPath, HttpMethod httpMethod, JsonNode requestBody) {
        return doCallAndThrowExceptionIfStatuscodeIsNot2xx(responseBodyClass, uriPath, httpMethod, null, requestBody);
    }

    public <T> T doCallAndThrowExceptionIfStatuscodeIsNot2xx(final Class<T> responseBodyClass, String uriPath, HttpMethod httpMethod,
                                                             Map<String, String> queryParams, JsonNode requestBody) {
        HttpHeaders httpHeaders = new HttpHeaders();
        BodyInserter<?, ? super ClientHttpRequest> requestBodyInserter;

        if (requestBody == null) {
            requestBodyInserter = BodyInserters.empty();
        } else {
            requestBodyInserter = BodyInserters.fromValue(requestBody.toString());
            httpHeaders.add("Content-Type", "application/json");
        }

        return doCallAndThrowExceptionIfStatuscodeIsNot2xx(responseBodyClass, uriPath, httpMethod, queryParams, requestBodyInserter, httpHeaders, null, false);
    }

    public <T> T doCallAndThrowExceptionIfStatuscodeIsNot2xx(final Class<T> responseBodyClass, String uriPath, HttpMethod httpMethod, HttpHeaders headers) {

        return doCallAndThrowExceptionIfStatuscodeIsNot2xx(responseBodyClass, uriPath, httpMethod, null, BodyInserters.empty(), headers, null, false);
    }

    public <T> T doCallAndThrowExceptionIfStatuscodeIsNot2xx(final Class<T> responseBodyClass, String uriPath, HttpMethod httpMethod, Map<String, String> queryParams,
                                                             BodyInserter<? extends Object, ReactiveHttpOutputMessage> requestBodyInserter, HttpHeaders headers) {

        return doCallAndThrowExceptionIfStatuscodeIsNot2xx(responseBodyClass, uriPath, httpMethod, queryParams, requestBodyInserter, headers, null, false);
    }

    public <T> T doCallAndThrowExceptionIfStatuscodeIsNot2xx(final Class<T> responseBodyClass, String uriPath, HttpMethod httpMethod, Map<String, String> queryParams,
                                                             String stringRequestBody, HttpHeaders headers) {
        return doCallAndThrowExceptionIfStatuscodeIsNot2xx(responseBodyClass, uriPath, httpMethod, queryParams,
                BodyInserters.fromValue(stringRequestBody), headers, null, false);
    }

    public <T> T doCallAndThrowExceptionIfStatuscodeIsNot2xx(final Class<T> responseBodyClass, String uriPath, HttpMethod httpMethod, Map<String, String> queryParams,
                                                             BodyInserter<?, ? super ClientHttpRequest> requestBodyInserter, HttpHeaders headers, MultiValueMap<String, String> cookies, final boolean disableUrlEncodingOfQueryParams) {
        if (requestBodyInserter == null)
            requestBodyInserter = BodyInserters.empty();

        String queryString = createQueryStringFromHttpParameters(queryParams, disableUrlEncodingOfQueryParams);
        String fullUriString = this.apiBaseUrl + uriPath + "?" + queryString;

        final Mono<T> responseBodyMono = this.webClient
                .method(httpMethod)
                .uri(fullUriString)
                .cookies(requestCookies -> {
                    if (cookies != null)
                        requestCookies.addAll(cookies);
                })
                .body(requestBodyInserter)
                .headers(httpHeaders -> {
                    if (headers != null)
                        httpHeaders.addAll(headers);
                })
                .exchange()
                .flatMap((clientResponse) -> {
                    if (clientResponse.statusCode().is2xxSuccessful()) {
                        return handleWhenResponseCodeIsSuccess(clientResponse, httpMethod, fullUriString, responseBodyClass);
                    } else {
                        return handleWhenResponseCodeIndicatesFailure(clientResponse, httpMethod, fullUriString);
                    }
                });

        return responseBodyMono.block();
    }

    protected String createQueryStringFromHttpParameters(final Map<String, String> queryParams, final boolean disableUrlEncodingOfQueryParams) {
        if (queryParams == null) {
            return "";
        } else {
            return queryParams.keySet().stream()
                    .map((key) -> generateKeyValuePairForHttpQueryString(queryParams, key, disableUrlEncodingOfQueryParams))
                    .collect(Collectors.joining("&"));
        }
    }

    private String generateKeyValuePairForHttpQueryString(final Map<String, String> allQueryParams, final String keyToGeneratePairFor, final boolean disableUrlEncodingOfQueryParams) {
        String value = allQueryParams.get(keyToGeneratePairFor);
        if (value == null) {
            return String.format("%s=", keyToGeneratePairFor);
        }

        if (disableUrlEncodingOfQueryParams) {
            return String.format("%s=%s", keyToGeneratePairFor, value);
        } else {
            return String.format("%s=%s", keyToGeneratePairFor, URLEncoder.encode(value, StandardCharsets.UTF_8));
        }
    }

    protected <T> Mono<T> handleWhenResponseCodeIsSuccess(ClientResponse response, HttpMethod httpMethod, String fullUriString, final Class<T> responseBodyClass) {
        return response
                .bodyToMono(responseBodyClass)
                .doOnSuccess((T responseBody) ->
                        log.debug("Rest call succeeded. Url: {{} {}}, HTTP status code: {{}}, Parsed responseBody.toString length: {{}}",
                                () -> httpMethod,
                                () -> hideSecretForLogsInString(fullUriString),
                                response::statusCode,
                                () -> responseBody != null ? responseBody.toString().length() : "<No response body was present>"
                        ))
                .doOnError(throwable ->
                        log.debug("Exception while getting response body. Expected body class type: {}, Url: {{} {}}, HTTP status code: {{}}",
                                responseBodyClass.getSimpleName(),
                                httpMethod,
                                hideSecretForLogsInString(fullUriString),
                                response.statusCode(), throwable
                        ));
    }

    protected <T> Mono<T> handleWhenResponseCodeIndicatesFailure(ClientResponse response, HttpMethod httpMethod, String fullUriString) {
        String locationHeader = "";
        if (response.statusCode().is3xxRedirection()) {
            if (!response.headers().header("Location").isEmpty()) {
                locationHeader = response.headers().header("Location").get(0);
            }
        }
        final String locationHeaderToLog = locationHeader.isEmpty() ? "" : "Header - Location: " + hideSecretForLogsInString(locationHeader) + "\n";


        final Mono monoToReturn = response
                .bodyToMono(String.class)
                .flatMap((String responseBodyString) -> {//Handling if ResponseBody is present
                    return Mono.error(ResponseCodeIsNotSuccessApiClientException.builder()
                            .message("HTTP status code indicates failure")
                            .httpStatus(response.statusCode())
                            .responseBody(responseBodyString)
                            .build());
                })
                .switchIfEmpty(//Handling if ResponseBody is NOT present
                        Mono.error(ResponseCodeIsNotSuccessApiClientException.builder()
                                .message("HTTP status code indicates failure")
                                .httpStatus(response.statusCode())
                                .responseBody(null)
                                .build())
                )
                .doOnError((throwable) -> {
                    if (throwable instanceof ResponseCodeIsNotSuccessApiClientException) {
                        final String responseBodyFromException = ((ResponseCodeIsNotSuccessApiClientException) throwable).getResponseBody();

                        final String responseBody = responseBodyFromException != null ? responseBodyFromException : "";
                        final String censoredResponseBody = hideSecretForLogsInString(responseBody);

                        log.error("HTTP status code indicates failure. Url: {{} {}}, HTTP status code: {{}}\nresponse:\n{}",
                                () -> httpMethod,
                                () -> hideSecretForLogsInString(fullUriString),
                                response::statusCode,
                                () -> locationHeaderToLog + censoredResponseBody
                        );
                    }
                });
        return monoToReturn;
    }

    public String hideSecretForLogsInString(String str) {
        if (str == null)
            return null;

        for (String secret : this.secretsToHideInLogs) {
            str = StringUtils.replace(str, secret, "[CENSORED]");
        }
        return str;
    }
}
