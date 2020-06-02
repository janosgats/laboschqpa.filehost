package com.laboschqpa.filehost.exceptions.apierrordescriptor;

import com.laboschqpa.filehost.enums.apierrordescriptor.FileServingApiError;

public class FileServingException extends ApiErrorDescriptorException {
    public FileServingException(FileServingApiError fileServingApiError) {
        super(fileServingApiError);
    }

    public FileServingException(FileServingApiError fileServingApiError, String message) {
        super(fileServingApiError, message);
    }

    public FileServingException(FileServingApiError fileServingApiError, String message, Throwable cause) {
        super(fileServingApiError, message, cause);
    }
}
