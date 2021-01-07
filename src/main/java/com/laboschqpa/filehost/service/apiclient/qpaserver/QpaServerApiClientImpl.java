package com.laboschqpa.filehost.service.apiclient.qpaserver;

import com.laboschqpa.filehost.annotation.ExceptionWrappedApiClient;
import com.laboschqpa.filehost.service.apiclient.AbstractApiClient;
import com.laboschqpa.filehost.service.apiclient.ApiCallerFactory;
import com.laboschqpa.filehost.service.apiclient.qpaserver.dto.IsUserAuthorizedToResourceRequestDto;
import com.laboschqpa.filehost.service.apiclient.qpaserver.dto.IsUserAuthorizedToResourceResponseDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;

@Service
@ExceptionWrappedApiClient
public class QpaServerApiClientImpl extends AbstractApiClient implements QpaServerApiClient {

    @Value("${apiClient.qpaServer.baseUrl}")
    private String qpaServerBaseUrl;

    @Value("${apiClient.qpaServer.sessionResolver.isUserAuthorizedToResource}")
    private String isAuthorizedToResourceUri;

    public QpaServerApiClientImpl(ApiCallerFactory apiCallerFactory) {
        super(apiCallerFactory, true);
    }

    @Override
    public IsUserAuthorizedToResourceResponseDto getIsUserAuthorizedToResource(IsUserAuthorizedToResourceRequestDto isUserAuthorizedToResourceRequestDto) {

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add(HttpHeaders.CONTENT_TYPE, "application/json");

        return getApiCaller().doCallAndThrowExceptionIfStatuscodeIsNot2xx(
                IsUserAuthorizedToResourceResponseDto.class,
                isAuthorizedToResourceUri,
                HttpMethod.GET,
                null,
                BodyInserters.fromValue(isUserAuthorizedToResourceRequestDto),
                httpHeaders
        );
    }


    @Override
    protected String getApiBaseUrl() {
        return qpaServerBaseUrl;
    }
}
