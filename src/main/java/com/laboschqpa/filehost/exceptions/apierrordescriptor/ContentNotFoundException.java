package com.laboschqpa.filehost.exceptions.apierrordescriptor;

import com.laboschqpa.filehost.enums.apierrordescriptor.ContentApiError;

public class ContentNotFoundException extends ApiErrorDescriptorException {
    public ContentNotFoundException(String message) {
        super(ContentApiError.CONTENT_IS_NOT_FOUND, message);
    }
}
