package com.laboschqpa.filehost.service.apiclient.qpaserver;

import com.laboschqpa.filehost.api.dto.IsUserAuthorizedToResourceResponseDto;

public interface QpaServerApiClient
{
    IsUserAuthorizedToResourceResponseDto getIsAuthorizedToResource(String sessionCookieValue, GetIsUserAuthorizedToResourceDto getIsUserAuthorizedToResourceDto);
}
