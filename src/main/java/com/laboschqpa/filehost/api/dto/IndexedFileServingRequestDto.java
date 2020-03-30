package com.laboschqpa.filehost.api.dto;

import com.laboschqpa.filehost.enums.FileAccessType;
import lombok.*;
import org.springframework.http.HttpMethod;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class IndexedFileServingRequestDto {
    private HttpMethod httpMethod;
    private String csrfToken;
    private Long indexedFileId;
    private FileAccessType fileAccessType;
}
