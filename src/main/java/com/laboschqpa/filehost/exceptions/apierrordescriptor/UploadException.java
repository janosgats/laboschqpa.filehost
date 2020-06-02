package com.laboschqpa.filehost.exceptions.apierrordescriptor;

import com.laboschqpa.filehost.enums.apierrordescriptor.UploadApiError;

public class UploadException extends ApiErrorDescriptorException {
    public UploadException(UploadApiError uploadApiError) {
        super(uploadApiError);
    }

    public UploadException(UploadApiError uploadApiError, String message) {
        super(uploadApiError, message);
    }

    public UploadException(UploadApiError uploadApiError, String message, Throwable cause) {
        super(uploadApiError, message, cause);
    }
}
