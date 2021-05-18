package com.laboschqpa.filehost.enums.attributeconverter;

import com.laboschqpa.filehost.enums.UploadType;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

@Converter
public class UploadTypeAttributeConverter implements AttributeConverter<UploadType, Integer> {
    @Override
    public Integer convertToDatabaseColumn(UploadType enumVal) {
        return enumVal.getValue();
    }

    @Override
    public UploadType convertToEntityAttribute(Integer val) {
        return UploadType.fromValue(val);
    }
}
