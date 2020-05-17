package com.laboschqpa.filehost.enums.attributeconverter;

import com.laboschqpa.filehost.enums.QuotaSubjectCategory;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

@Converter
public class QuotaSubjectCategoryAttributeConverter implements AttributeConverter<QuotaSubjectCategory, Integer> {
    @Override
    public Integer convertToDatabaseColumn(QuotaSubjectCategory enumVal) {
        return enumVal.getValue();
    }

    @Override
    public QuotaSubjectCategory convertToEntityAttribute(Integer val) {
        return QuotaSubjectCategory.fromValue(val);
    }
}
