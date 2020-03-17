package com.laboschcst.filehost.service.apiclient;

import com.laboschcst.filehost.api.dto.InternalResourceDto;
import com.laboschcst.filehost.api.dto.IsUserAuthorizedToResourceResponseDto;

public interface CsillagturaServerApiClient {
    IsUserAuthorizedToResourceResponseDto getIsAuthorizedToResource(String sessionCookieValue, InternalResourceDto internalResourceDto);
}
