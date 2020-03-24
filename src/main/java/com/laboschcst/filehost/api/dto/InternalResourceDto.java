package com.laboschcst.filehost.api.dto;

import com.laboschcst.filehost.enums.FileAccessType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class InternalResourceDto {
    private Long storedFileId;
    private FileAccessType fileAccessType;
}
