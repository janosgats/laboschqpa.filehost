package com.laboschcst.filehost.service.apiclient;

public interface CsillagturaServerApiClient {
    boolean getIfUserIsAuthorizedToResource(String sessionCookieValue, String resourceId);
}
