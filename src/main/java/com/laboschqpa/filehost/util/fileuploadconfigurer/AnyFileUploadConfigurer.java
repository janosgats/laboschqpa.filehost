package com.laboschqpa.filehost.util.fileuploadconfigurer;

import com.laboschqpa.filehost.model.file.UploadableFile;
import com.laboschqpa.filehost.model.inputstream.TrackingInputStream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;


@Service
public class AnyFileUploadConfigurer implements FileUploadConfigurer {

    @Value("${filehost.upload.filemaxsize.any}")
    private Long uploadFileMaxSize;

    public void applyMaxFileSize(TrackingInputStream trackingInputStream) {
        trackingInputStream.setLimit(uploadFileMaxSize);
    }


    public void assertMimeType(UploadableFile newUploadableFile) {
        //doing nothing
    }
}
