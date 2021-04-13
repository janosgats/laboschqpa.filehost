package com.laboschqpa.filehost.util.fileuploadconfigurer;

import com.laboschqpa.filehost.enums.UploadedFileType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class FileUploadConfigurerFactory {
    private final AnyFileUploadConfigurer anyFileUploadConfigurer;
    private final ImageFileUploadConfigurer imageFileUploadConfigurer;

    public FileUploadConfigurer get(UploadedFileType uploadedFileType) {
        switch (uploadedFileType) {
            case ANY:
                return anyFileUploadConfigurer;
            case IMAGE:
                return imageFileUploadConfigurer;
            default:
                throw new IllegalStateException("Unexpected value: " + uploadedFileType);
        }
    }
}
