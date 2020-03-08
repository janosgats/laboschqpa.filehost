package com.laboschcst.filehost.service.apiclient;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.laboschcst.filehost.config.annotation.HandledApiClient;
import com.laboschcst.filehost.exceptions.ApiClientException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Service
@RequiredArgsConstructor
@HandledApiClient
public class CsillagturaServerApiClientImpl extends AbstractApiClient implements CsillagturaServerApiClient {
    private static final Logger logger = LoggerFactory.getLogger(CsillagturaServerApiClientImpl.class);

    private final WebClient webClient;

    @Value("${apiClient.csillagturaServer.baseUrl}")
    private String affiliatesPayoutBaseUrl;

    @Value("${apiClient.csillagturaServer.sessionResolver.isAuthorizedToResource}")
    private String isAuthorizedToResourceUri;

    @Override
    public boolean getIfUserIsAuthorizedToResource(String sessionCookieValue, String resourceId) {
        LinkedMultiValueMap<String, String> cookies = new LinkedMultiValueMap();
        cookies.add("SESSION", sessionCookieValue);
        String responseBody;
        try {
            responseBody = doCallAndThrowExceptionIfStatuscodeIsNot2xx(
                    isAuthorizedToResourceUri,
                    HttpMethod.GET,
                    Map.of("resourceId", resourceId),
                    null,
                    cookies);
        } catch (ApiClientException e) {
            if (e.getHttpStatus() != null && e.getHttpStatus().is3xxRedirection())
                return false;//The user isn't logged in so got redirected.
            else
                throw e;
        }

        ObjectNode responseObjectNode = (ObjectNode) convertStringToJsonNode(responseBody);
        return responseObjectNode.get("isAuthorized").asBoolean();
    }


    @Override
    protected String getApiBaseUrl() {
        return affiliatesPayoutBaseUrl;
    }

    @Override
    protected WebClient getWebClient() {
        return webClient;
    }
}
