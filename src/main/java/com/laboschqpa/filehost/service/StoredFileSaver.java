package com.laboschqpa.filehost.service;

import com.laboschqpa.filehost.entity.StoredFileEntity;
import com.laboschqpa.filehost.model.streamtracking.TrackingInputStream;

import java.io.File;

public interface StoredFileSaver {
    void writeFromStream(TrackingInputStream fileUploadingInputStream, File targetFile, StoredFileEntity storedFileEntity, Long approximateFileSize);
}
