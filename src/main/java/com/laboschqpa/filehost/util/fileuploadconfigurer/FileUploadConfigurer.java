package com.laboschqpa.filehost.util.fileuploadconfigurer;

import com.laboschqpa.filehost.model.file.UploadableFile;
import com.laboschqpa.filehost.model.inputstream.TrackingInputStream;

public interface FileUploadConfigurer {

    void applyMaxFileSize(TrackingInputStream trackingInputStream);

    void assertMimeType(UploadableFile newUploadableFile);
}
