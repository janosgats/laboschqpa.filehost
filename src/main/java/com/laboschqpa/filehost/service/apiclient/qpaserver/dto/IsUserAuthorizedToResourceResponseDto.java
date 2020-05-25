package com.laboschqpa.filehost.service.apiclient.qpaserver.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class IsUserAuthorizedToResourceResponseDto {
    @Builder.Default
    private boolean authenticated = true;
    @Builder.Default
    private boolean authorized = false;
    @Builder.Default
    private boolean csrfValid = true;

    private Long loggedInUserId;
    private Long loggedInUserTeamId;
}

