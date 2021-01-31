package com.laboschqpa.filehost.service;

import com.laboschqpa.filehost.entity.S3FileEntity;

import java.io.InputStream;

public interface S3FileSaver {
    void writeFromStream(InputStream fileUploadingInputStream, S3FileEntity s3FileEntity);
}
