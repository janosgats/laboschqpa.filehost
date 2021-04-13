package com.laboschqpa.filehost.util.fileuploadconfigurer;

import com.laboschqpa.filehost.enums.apierrordescriptor.UploadApiError;
import com.laboschqpa.filehost.exceptions.apierrordescriptor.UploadException;
import com.laboschqpa.filehost.model.file.UploadableFile;
import com.laboschqpa.filehost.model.inputstream.TrackingInputStream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;


@Service
public class ImageFileUploadConfigurer implements FileUploadConfigurer {
    private static final String MIME_TYPE_IMAGE = "image";

    @Value("${filehost.upload.filemaxsize.image}")
    private Long uploadFileMaxSize;

    public void applyMaxFileSize(TrackingInputStream trackingInputStream) {
        trackingInputStream.setLimit(uploadFileMaxSize);
    }


    public void assertMimeType(UploadableFile newUploadableFile) {
        if (StringUtils.startsWithIgnoreCase(newUploadableFile.getMimeType(), MIME_TYPE_IMAGE)) {
            return;
        }

        throw new UploadException(UploadApiError.MIME_TYPE_IS_NOT_IMAGE);
    }
}
