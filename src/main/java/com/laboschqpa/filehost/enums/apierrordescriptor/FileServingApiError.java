package com.laboschqpa.filehost.enums.apierrordescriptor;

import com.laboschqpa.filehost.annotation.ApiErrorCategory;
import com.laboschqpa.filehost.api.errorhandling.ApiErrorDescriptor;

@ApiErrorCategory("fileServing")
public enum FileServingApiError implements ApiErrorDescriptor {
    FILE_IS_NOT_AVAILABLE(1),
    INVALID_STORED_FILE(2),
    INVALID_UPLOAD_REQUEST(3),
    FILE_DOES_NOT_EXIST(4),
    CANNOT_CREATE_FILE_READ_STREAM(5),
    CANNOT_DELETE_FILE(6),
    FILE_STATUS_IS_ALREADY_DELETED(7);

    private Integer apiErrorCode;

    FileServingApiError(Integer apiErrorCode) {
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
