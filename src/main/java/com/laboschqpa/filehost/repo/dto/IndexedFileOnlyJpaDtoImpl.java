package com.laboschqpa.filehost.repo.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Setter
@Getter(onMethod_ = {@Override})
public class IndexedFileOnlyJpaDtoImpl implements IndexedFileOnlyJpaDto {
    private Long id;
    private Integer dType;
    private Integer statusVal;
    private Long ownerUserId;
    private Long ownerTeamId;
    private Instant creationTime;
    private String mimeType;
    private String name;
    private Long size;
}
