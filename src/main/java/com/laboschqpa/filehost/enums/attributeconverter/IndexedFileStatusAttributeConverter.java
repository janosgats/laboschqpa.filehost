package com.laboschqpa.filehost.enums.attributeconverter;

import com.laboschqpa.filehost.enums.IndexedFileStatus;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

@Converter
public class IndexedFileStatusAttributeConverter implements AttributeConverter<IndexedFileStatus, Integer> {
    @Override
    public Integer convertToDatabaseColumn(IndexedFileStatus enumVal) {
        return enumVal.getValue();
    }

    @Override
    public IndexedFileStatus convertToEntityAttribute(Integer val) {
        return IndexedFileStatus.fromValue(val);
    }
}
