package com.laboschqpa.filehost.exceptions.apierrordescriptor;

import com.laboschqpa.filehost.enums.apierrordescriptor.FileServingApiError;

public class InvalidUploadRequestException extends ApiErrorDescriptorException {
    public InvalidUploadRequestException(String message) {
        super(FileServingApiError.INVALID_UPLOAD_REQUEST, message);
    }
}
