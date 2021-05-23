package com.laboschqpa.filehost.enums.attributeconverter;

import com.laboschqpa.filehost.enums.ImageVariantStatus;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

@Converter
public class ImageVariantStatusAttributeConverter implements AttributeConverter<ImageVariantStatus, Integer> {
    @Override
    public Integer convertToDatabaseColumn(ImageVariantStatus enumVal) {
        return enumVal.getValue();
    }

    @Override
    public ImageVariantStatus convertToEntityAttribute(Integer val) {
        return ImageVariantStatus.fromValue(val);
    }
}
