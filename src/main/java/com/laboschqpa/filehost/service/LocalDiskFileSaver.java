package com.laboschqpa.filehost.service;

import com.laboschqpa.filehost.entity.LocalDiskFileEntity;
import com.laboschqpa.filehost.model.inputstream.CountingInputStream;

import java.io.File;

public interface LocalDiskFileSaver {
    void writeFromStream(CountingInputStream fileUploadingInputStream, File targetFile, LocalDiskFileEntity localDiskFileEntity);
}
