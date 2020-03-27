package com.laboschqpa.filehost.service.apiclient;

import com.laboschqpa.filehost.api.dto.IndexedFileServingRequestDto;
import com.laboschqpa.filehost.api.dto.IsUserAuthorizedToResourceResponseDto;

public interface QpaServerApiClient
{
    IsUserAuthorizedToResourceResponseDto getIsAuthorizedToResource(String sessionCookieValue, IndexedFileServingRequestDto indexedFileServingRequestDto);
}
