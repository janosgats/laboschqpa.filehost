package com.laboschqpa.filehost.model.quota;

import com.laboschqpa.filehost.enums.QuotaSubjectCategory;
import com.laboschqpa.filehost.enums.attributeconverter.QuotaSubjectCategoryAttributeConverter;

public interface QuotaResourceUsageJpaDto {
    QuotaSubjectCategoryAttributeConverter QUOTA_SUBJECT_CATEGORY_ATTRIBUTE_CONVERTER = new QuotaSubjectCategoryAttributeConverter();

    Long getSubjectId();

    Integer getSubjectCategoryVal();

    default QuotaSubjectCategory getSubjectCategory() {
        return QUOTA_SUBJECT_CATEGORY_ATTRIBUTE_CONVERTER.convertToEntityAttribute(getSubjectCategoryVal());
    }

    Long getUsedBytes();
}
