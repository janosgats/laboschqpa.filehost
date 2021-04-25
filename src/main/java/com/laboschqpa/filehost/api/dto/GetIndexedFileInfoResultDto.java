package com.laboschqpa.filehost.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.laboschqpa.filehost.enums.IndexedFileStatus;
import com.laboschqpa.filehost.repo.dto.IndexedFileOnlyJpaDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GetIndexedFileInfoResultDto {
    @JsonProperty("isExisting")
    private boolean isExisting;
    private Long indexedFileId;
    private IndexedFileStatus indexedFileStatus;
    private Long ownerUserId;
    private Long ownerTeamId;
    private Instant creationTime;
    private String mimeType;
    private String originalFileName;

    public GetIndexedFileInfoResultDto(Long indexedFileId, boolean isExisting) {
        this.indexedFileId = indexedFileId;
        this.isExisting = isExisting;
    }

    public GetIndexedFileInfoResultDto(IndexedFileOnlyJpaDto indexedFileOnlyJpaDto) {
        this.isExisting = true;
        this.indexedFileId = indexedFileOnlyJpaDto.getId();
        this.indexedFileStatus = indexedFileOnlyJpaDto.getStatus();
        this.ownerUserId = indexedFileOnlyJpaDto.getOwnerUserId();
        this.ownerTeamId = indexedFileOnlyJpaDto.getOwnerTeamId();
        this.creationTime = indexedFileOnlyJpaDto.getCreationTime();
        this.mimeType = indexedFileOnlyJpaDto.getMimeType();
        this.originalFileName = indexedFileOnlyJpaDto.getOriginalFileName();
    }
}
