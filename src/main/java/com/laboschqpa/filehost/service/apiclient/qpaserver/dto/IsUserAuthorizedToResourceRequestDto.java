package com.laboschqpa.filehost.service.apiclient.qpaserver.dto;

import com.laboschqpa.filehost.enums.FileAccessType;
import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class IsUserAuthorizedToResourceRequestDto {
    /**
     * Unencoded (NOT Base64 encoded) session ID
     */
    private String sessionId;
    private String csrfToken;
    private FileAccessType fileAccessType;
    private Long indexedFileId;
    private Long indexedFileOwnerUserId;
    private Long indexedFileOwnerTeamId;
}