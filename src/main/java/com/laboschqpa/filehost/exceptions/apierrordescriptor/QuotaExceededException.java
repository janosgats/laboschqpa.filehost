package com.laboschqpa.filehost.exceptions.apierrordescriptor;

import com.laboschqpa.filehost.enums.apierrordescriptor.QuotaExceededApiError;
import com.laboschqpa.filehost.exceptions.apierrordescriptor.ApiErrorDescriptorException;

public class QuotaExceededException extends ApiErrorDescriptorException {
    public QuotaExceededException(QuotaExceededApiError quotaExceededApiError) {
        this(quotaExceededApiError, null);
    }

    public QuotaExceededException(QuotaExceededApiError quotaExceededApiError, String message) {
        super(quotaExceededApiError, message);
    }
}
