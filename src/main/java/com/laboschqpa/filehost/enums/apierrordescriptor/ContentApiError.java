package com.laboschqpa.filehost.enums.apierrordescriptor;

import com.laboschqpa.filehost.annotation.ApiErrorCategory;
import com.laboschqpa.filehost.api.errorhandling.ApiErrorDescriptor;

@ApiErrorCategory("content")
public enum ContentApiError implements ApiErrorDescriptor {
    CONTENT_IS_NOT_FOUND(1);

    private Integer apiErrorCode;

    ContentApiError(Integer apiErrorCode) {
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
