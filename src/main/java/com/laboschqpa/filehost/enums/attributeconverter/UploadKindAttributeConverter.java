package com.laboschqpa.filehost.enums.attributeconverter;

import com.laboschqpa.filehost.enums.UploadKind;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

@Converter
public class UploadKindAttributeConverter implements AttributeConverter<UploadKind, Integer> {
    @Override
    public Integer convertToDatabaseColumn(UploadKind enumVal) {
        return enumVal.getValue();
    }

    @Override
    public UploadKind convertToEntityAttribute(Integer val) {
        return UploadKind.fromValue(val);
    }
}
