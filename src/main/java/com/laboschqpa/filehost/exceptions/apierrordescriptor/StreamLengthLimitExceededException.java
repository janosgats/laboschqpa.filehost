package com.laboschqpa.filehost.exceptions.apierrordescriptor;

import com.laboschqpa.filehost.enums.apierrordescriptor.UploadApiError;
import com.laboschqpa.filehost.exceptions.apierrordescriptor.ApiErrorDescriptorException;

public class StreamLengthLimitExceededException extends ApiErrorDescriptorException {
    public StreamLengthLimitExceededException(String message) {
        super(UploadApiError.STREAM_LENGTH_LIMIT_EXCEEDED, message);
    }
}
