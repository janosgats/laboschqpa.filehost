package com.laboschqpa.filehost.enums.apierrordescriptor;

import com.laboschqpa.filehost.annotation.ApiErrorCategory;
import com.laboschqpa.filehost.api.errorhandling.ApiErrorDescriptor;

@ApiErrorCategory("quotaExceeded")
public enum QuotaExceededApiError implements ApiErrorDescriptor {
    USER_QUOTA_EXCEEDED(1),
    TEAM_QUOTA_EXCEEDED(2);

    private Integer apiErrorCode;

    QuotaExceededApiError(Integer apiErrorCode) {
        this.apiErrorCode = apiErrorCode;
    }

    @Override
    public Integer getApiErrorCode() {
        return apiErrorCode;
    }

    @Override
    public String getApiErrorName() {
        return toString();
    }
}
