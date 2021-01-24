package com.laboschqpa.filehost.service;

import com.laboschqpa.filehost.entity.LocalDiskFileEntity;

import java.io.File;
import java.io.InputStream;

public interface LocalDiskFileSaver {
    void writeFromStream(InputStream fileUploadingInputStream, File targetFile, LocalDiskFileEntity localDiskFileEntity);
}
