package com.laboschqpa.filehost.api.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class FileUploadResponse {
    private Long createdFileId;
    private String mimeType;
}
