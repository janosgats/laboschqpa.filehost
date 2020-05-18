package com.laboschqpa.filehost.repo.dto;

import com.laboschqpa.filehost.enums.IndexedFileStatus;
import com.laboschqpa.filehost.enums.attributeconverter.IndexedFileStatusAttributeConverter;

public interface IndexedFileOnlyJpaDto {
    IndexedFileStatusAttributeConverter INDEXED_FILE_STATUS_ATTRIBUTE_CONVERTER = new IndexedFileStatusAttributeConverter();

    Long getId();

    Integer getDType();

    Integer getStatusVal();

    default IndexedFileStatus getStatus() {
        return INDEXED_FILE_STATUS_ATTRIBUTE_CONVERTER.convertToEntityAttribute(getStatusVal());
    }

    Long getOwnerUserId();

    Long getOwnerTeamId();

    String getMimeType();
}
