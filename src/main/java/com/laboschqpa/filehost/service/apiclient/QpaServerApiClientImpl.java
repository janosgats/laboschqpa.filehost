package com.laboschqpa.filehost.service.apiclient;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.laboschqpa.filehost.api.dto.InternalResourceDto;
import com.laboschqpa.filehost.api.dto.IsUserAuthorizedToResourceResponseDto;
import com.laboschqpa.filehost.config.annotation.HandledApiClient;
import com.laboschqpa.filehost.exceptions.ApiClientException;
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
public class QpaServerApiClientImpl extends AbstractApiClient implements QpaServerApiClient
{
    private static final Logger logger = LoggerFactory.getLogger(QpaServerApiClientImpl.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final WebClient webClient;

    @Value("${apiClient.qpaServer.baseUrl}")
    private String qpaServerBaseUrl;

    @Value("${apiClient.qpaServer.sessionResolver.isUserAuthorizedToResource}")
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
        return qpaServerBaseUrl;
    }

    @Override
    protected WebClient getWebClient() {
        return webClient;
    }
}
