package com.laboschqpa.filehost.config.filter;

import com.laboschqpa.filehost.enums.FileAccessType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class WrappedFileServingRequestDto {
    private Long ownerUserId;
    private Long ownerTeamId;
    private Long indexedFileId;
    private FileAccessType fileAccessType;
}

