package com.laboschqpa.filehost.enums.attributeconverter;

import com.laboschqpa.filehost.enums.S3Provider;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

@Converter
public class S3ProviderAttributeConverter implements AttributeConverter<S3Provider, Integer> {
    @Override
    public Integer convertToDatabaseColumn(S3Provider enumVal) {
        return enumVal.getValue();
    }

    @Override
    public S3Provider convertToEntityAttribute(Integer val) {
        return S3Provider.fromValue(val);
    }
}
