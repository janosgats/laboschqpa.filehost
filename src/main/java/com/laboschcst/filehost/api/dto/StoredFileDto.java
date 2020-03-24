package com.laboschcst.filehost.api.dto;

import com.laboschcst.filehost.enums.StoredFileStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class StoredFileDto {
    private Long id;
    private StoredFileStatus status;
    private String path;
    private Long originalUploaderUserId;
    private Long currentUploaderUserId;
    private Long size;
}