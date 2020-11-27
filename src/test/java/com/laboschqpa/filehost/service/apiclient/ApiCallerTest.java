package com.laboschqpa.filehost.service.apiclient;

import com.laboschqpa.filehost.config.AppConfig;
import com.laboschqpa.filehost.exceptions.apiclient.ResponseCodeIsNotSuccessApiClientException;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ReactiveHttpOutputMessage;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.BodyInserter;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.spy;

class ApiCallerTest {
    private static final String[] secretsToHideInLogs = {"testSecretToHideInLog"};

    private WebClient webClient = new AppConfig().webClient();
    private ApiCaller apiCaller;

    private static MockWebServer mockWebServer;

    @BeforeAll
    static void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
    }

    @AfterAll
    static void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @BeforeEach
    void beforeEach() {
        apiCaller = spy(new ApiCaller("http://localhost:" + mockWebServer.getPort(), webClient, secretsToHideInLogs));
    }

    @Test
    void doCallAndThrowExceptionIfStatuscodeIsNot2xx_success_withResponseBody() throws InterruptedException {
        final Class<String> responseBodyClass = String.class;
        final String uriPath = "/test";
        final HttpMethod httpMethod = HttpMethod.POST;
        final Map<String, String> queryParams = Map.of("param1", "value1");
        final String requestBodyString = "request body string";
        final BodyInserter<String, ReactiveHttpOutputMessage> requestBodyInserter = BodyInserters.fromValue(requestBodyString);
        final HttpHeaders headers = new HttpHeaders();
        final MultiValueMap<String, String> cookies = new LinkedMultiValueMap<>();
        cookies.add("keyA", "valueA");
        headers.add("key1", "value1");
        final boolean disableUrlEncodingOfQueryParams = false;

        final String expectedResponseBody = "expected response body is here";

        mockWebServer.enqueue(new MockResponse()
                .setBody(expectedResponseBody));

        final String actualResponseBody
                = apiCaller.doCallAndThrowExceptionIfStatuscodeIsNot2xx(responseBodyClass, uriPath, httpMethod,
                queryParams, requestBodyInserter, headers, cookies, disableUrlEncodingOfQueryParams);

        final RecordedRequest recordedRequest = Objects.requireNonNull(mockWebServer.takeRequest(1, TimeUnit.SECONDS));


        assertTrue(Objects.requireNonNull(recordedRequest.getPath()).startsWith(uriPath + "?"));
        assertTrue(Objects.requireNonNull(recordedRequest.getPath()).contains("param1=value1"));

        assertEquals(httpMethod.name(), recordedRequest.getMethod());
        assertEquals("value1", recordedRequest.getHeader("key1"));
        assertEquals("keyA=valueA", recordedRequest.getHeader("Cookie"));

        assertEquals(requestBodyString, new String(recordedRequest.getBody().readByteArray()));
        assertEquals(expectedResponseBody, actualResponseBody);
    }

    @Test
    void doCallAndThrowExceptionIfStatuscodeIsNot2xx_success_withNoResponseBody() throws InterruptedException {
        final Class<String> responseBodyClass = String.class;
        final String uriPath = "/test";
        final HttpMethod httpMethod = HttpMethod.POST;
        final Map<String, String> queryParams = Map.of("param1", "value1");
        final String requestBodyString = "request body string";
        final BodyInserter<String, ReactiveHttpOutputMessage> requestBodyInserter = BodyInserters.fromValue(requestBodyString);
        final HttpHeaders headers = new HttpHeaders();
        final MultiValueMap<String, String> cookies = new LinkedMultiValueMap<>();
        cookies.add("keyA", "valueA");
        headers.add("key1", "value1");
        final boolean disableUrlEncodingOfQueryParams = false;

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200));

        final String actualResponseBody
                = apiCaller.doCallAndThrowExceptionIfStatuscodeIsNot2xx(responseBodyClass, uriPath, httpMethod,
                queryParams, requestBodyInserter, headers, cookies, disableUrlEncodingOfQueryParams);

        final RecordedRequest recordedRequest = Objects.requireNonNull(mockWebServer.takeRequest(1, TimeUnit.SECONDS));


        assertTrue(Objects.requireNonNull(recordedRequest.getPath()).startsWith(uriPath + "?"));
        assertTrue(Objects.requireNonNull(recordedRequest.getPath()).contains("param1=value1"));

        assertEquals(httpMethod.name(), recordedRequest.getMethod());
        assertEquals("value1", recordedRequest.getHeader("key1"));
        assertEquals("keyA=valueA", recordedRequest.getHeader("Cookie"));

        assertEquals(requestBodyString, new String(recordedRequest.getBody().readByteArray()));
        assertNull(actualResponseBody);
    }

    @Test
    void doCallAndThrowExceptionIfStatuscodeIsNot2xx_errorHttpResponseCodeIsNot2xx_withResponseBody() throws InterruptedException {
        final Class<String> responseBodyClass = String.class;
        final String uriPath = "/test";
        final HttpMethod httpMethod = HttpMethod.POST;
        final Map<String, String> queryParams = Map.of("param1", "value1");
        final String requestBodyString = "request body string";
        final BodyInserter<String, ReactiveHttpOutputMessage> requestBodyInserter = BodyInserters.fromValue(requestBodyString);
        final HttpHeaders headers = new HttpHeaders();
        final MultiValueMap<String, String> cookies = new LinkedMultiValueMap<>();
        cookies.add("keyA", "valueA");
        headers.add("key1", "value1");
        final boolean disableUrlEncodingOfQueryParams = false;

        final HttpStatus expectedResponseCode = HttpStatus.NOT_FOUND;
        final String expectedResponseBody = "expected response body is here";

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(expectedResponseCode.value())
                .setBody(expectedResponseBody));

        ResponseCodeIsNotSuccessApiClientException receivedException = null;
        try {
            apiCaller.doCallAndThrowExceptionIfStatuscodeIsNot2xx(responseBodyClass, uriPath, httpMethod,
                    queryParams, requestBodyInserter, headers, cookies, disableUrlEncodingOfQueryParams);
            fail("Nothing was thrown, but ResponseCodeIsNotSuccessApiClientException should have been thrown!");
        } catch (ResponseCodeIsNotSuccessApiClientException e) {
            receivedException = e;
        }

        mockWebServer.takeRequest(1, TimeUnit.SECONDS);

        assertEquals(expectedResponseCode, receivedException.getHttpStatus());
        assertEquals(expectedResponseBody, receivedException.getResponseBody());
    }

    @Test
    void doCallAndThrowExceptionIfStatuscodeIsNot2xx_errorHttpResponseCodeIsNot2xx_withNoResponseBody() throws InterruptedException {
        final Class<String> responseBodyClass = String.class;
        final String uriPath = "/test";
        final HttpMethod httpMethod = HttpMethod.POST;
        final Map<String, String> queryParams = Map.of("param1", "value1");
        final String requestBodyString = "request body string";
        final BodyInserter<String, ReactiveHttpOutputMessage> requestBodyInserter = BodyInserters.fromValue(requestBodyString);
        final HttpHeaders headers = new HttpHeaders();
        final MultiValueMap<String, String> cookies = new LinkedMultiValueMap<>();
        cookies.add("keyA", "valueA");
        headers.add("key1", "value1");
        final boolean disableUrlEncodingOfQueryParams = false;

        final HttpStatus expectedResponseCode = HttpStatus.NOT_FOUND;

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(expectedResponseCode.value()));

        ResponseCodeIsNotSuccessApiClientException receivedException = null;
        try {
            apiCaller.doCallAndThrowExceptionIfStatuscodeIsNot2xx(responseBodyClass, uriPath, httpMethod,
                    queryParams, requestBodyInserter, headers, cookies, disableUrlEncodingOfQueryParams);
            fail("Nothing was thrown, but ResponseCodeIsNotSuccessApiClientException should have been thrown!");
        } catch (ResponseCodeIsNotSuccessApiClientException e) {
            receivedException = e;
        }

        mockWebServer.takeRequest(1, TimeUnit.SECONDS);

        assertEquals(expectedResponseCode, receivedException.getHttpStatus());
        assertTrue(StringUtils.isEmpty(receivedException.getResponseBody()));
    }

    @Test
    void hideSecretForLogsInString() {
        final String str = "blahblahsa" + secretsToHideInLogs[0] + "asdkdjfasd" + secretsToHideInLogs[0];

        assertFalse(apiCaller.hideSecretForLogsInString(str).contains(secretsToHideInLogs[0]));
    }
}