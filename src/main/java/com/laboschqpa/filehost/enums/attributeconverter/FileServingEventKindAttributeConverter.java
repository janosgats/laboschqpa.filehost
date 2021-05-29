package com.laboschqpa.filehost.enums.attributeconverter;

import com.laboschqpa.filehost.enums.FileServingEventKind;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

@Converter
public class FileServingEventKindAttributeConverter implements AttributeConverter<FileServingEventKind, Integer> {
    @Override
    public Integer convertToDatabaseColumn(FileServingEventKind enumVal) {
        return enumVal.getValue();
    }

    @Override
    public FileServingEventKind convertToEntityAttribute(Integer val) {
        return FileServingEventKind.fromValue(val);
    }
}
