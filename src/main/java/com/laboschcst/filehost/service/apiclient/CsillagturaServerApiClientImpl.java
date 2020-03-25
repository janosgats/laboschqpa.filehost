package com.laboschcst.filehost.service.apiclient;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.laboschcst.filehost.api.dto.InternalResourceDto;
import com.laboschcst.filehost.api.dto.IsUserAuthorizedToResourceResponseDto;
import com.laboschcst.filehost.config.annotation.HandledApiClient;
import com.laboschcst.filehost.exceptions.ApiClientException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;

@Service
@RequiredArgsConstructor
@HandledApiClient
public class CsillagturaServerApiClientImpl extends AbstractApiClient implements CsillagturaServerApiClient {
    private static final Logger logger = LoggerFactory.getLogger(CsillagturaServerApiClientImpl.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final WebClient webClient;

    @Value("${apiClient.csillagturaServer.baseUrl}")
    private String csillagturaServerBaseUrl;

    @Value("${apiClient.csillagturaServer.sessionResolver.isUserAuthorizedToResource}")
    private String isAuthorizedToResourceUri;

    @Override
    public IsUserAuthorizedToResourceResponseDto getIsAuthorizedToResource(String sessionCookieValue, InternalResourceDto internalResourceDto) {
        LinkedMultiValueMap<String, String> cookies = new LinkedMultiValueMap();
        cookies.add("SESSION", sessionCookieValue);

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add("AuthInterService", System.getProperty("auth.interservice.key"));
        httpHeaders.add(HttpHeaders.CONTENT_TYPE, "application/json");
        try {
            return doCallAndThrowExceptionIfStatuscodeIsNot2xx(
                    IsUserAuthorizedToResourceResponseDto.class,
                    isAuthorizedToResourceUri,
                    HttpMethod.GET,
                    null,
                    objectMapper.writeValueAsString(internalResourceDto),
                    httpHeaders,
                    cookies
            );
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Exception while converting internalResourceDto to JSON for request body!", e);
        } catch (ApiClientException e) {
            if (e.getHttpStatus() != null && e.getHttpStatus().is3xxRedirection())
                return IsUserAuthorizedToResourceResponseDto.builder().authenticated(false).authorized(false).build();//The user isn't logged in so got redirected.
            else
                throw e;
        }
    }


    @Override
    protected String getApiBaseUrl() {
        return csillagturaServerBaseUrl;
    }

    @Override
    protected WebClient getWebClient() {
        return webClient;
    }
}
