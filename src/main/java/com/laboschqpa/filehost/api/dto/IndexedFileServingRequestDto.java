package com.laboschqpa.filehost.api.dto;

import com.laboschqpa.filehost.enums.FileAccessType;
import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class IndexedFileServingRequestDto {
    private Long indexedFileId;
    private FileAccessType fileAccessType;
}
