package com.laboschqpa.filehost.enums.apierrordescriptor;

import com.laboschqpa.filehost.annotation.ApiErrorCategory;
import com.laboschqpa.filehost.api.errorhandling.ApiErrorDescriptor;

@ApiErrorCategory("upload")
public enum UploadApiError implements ApiErrorDescriptor {
    ERROR_DURING_QUOTA_ALLOCATION(1),
    STREAM_LENGTH_LIMIT_EXCEEDED(2),
    ERROR_DURING_SAVING_FILE(3),
    CANNOT_WRITE_STREAM_TO_FILE(4);

    private Integer apiErrorCode;

    UploadApiError(Integer apiErrorCode) {
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
