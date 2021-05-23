package com.laboschqpa.filehost.service.apiclient.imageconverter.dto;

import lombok.Data;

@Data
public class ProcessCreationJobRequestDto {
    private Long jobId;
    private Long originalFileId;
    private Integer variantSize;
}
