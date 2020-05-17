package com.laboschqpa.filehost.exceptions.fileserving;

import com.laboschqpa.filehost.enums.QuotaSubjectCategory;
import lombok.Getter;

public class QuotaExceededException extends FileServingException {
    @Getter
    private final QuotaSubjectCategory quotaSubjectCategory;

    public QuotaExceededException(QuotaSubjectCategory quotaSubjectCategory) {
        this.quotaSubjectCategory = quotaSubjectCategory;
    }

    public QuotaExceededException(QuotaSubjectCategory quotaSubjectCategory, String message) {
        super(message);
        this.quotaSubjectCategory = quotaSubjectCategory;
    }
}
