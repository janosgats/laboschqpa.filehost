package com.laboschqpa.filehost.service.apiclient.qpaserver;

import com.laboschqpa.filehost.enums.FileAccessType;
import lombok.*;
import org.springframework.http.HttpMethod;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class GetIsUserAuthorizedToResourceDto {
    private HttpMethod httpMethod;
    private String csrfToken;
    private Long indexedFileId;
    private FileAccessType fileAccessType;
}