package com.laboschqpa.filehost.service.apiclient.qpaserver;

import com.laboschqpa.filehost.service.apiclient.qpaserver.dto.IsUserAuthorizedToResourceRequestDto;
import com.laboschqpa.filehost.service.apiclient.qpaserver.dto.IsUserAuthorizedToResourceResponseDto;

public interface QpaServerApiClient
{
    IsUserAuthorizedToResourceResponseDto getIsUserAuthorizedToResource(IsUserAuthorizedToResourceRequestDto isUserAuthorizedToResourceRequestDto);
}
