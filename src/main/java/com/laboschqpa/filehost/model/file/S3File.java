package com.laboschqpa.filehost.model.file;

import com.laboschqpa.filehost.entity.S3FileEntity;
import com.laboschqpa.filehost.service.S3FileSaver;
import lombok.extern.log4j.Log4j2;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;

import java.io.InputStream;

@Log4j2
public class S3File extends AbstractIndexedFile<S3FileEntity> implements UploadableFile, DeletableFile {
    private final S3FileSaver s3FileSaver;
    private final S3Client s3Client;

    public S3File(S3FileEntity s3FileEntity, S3FileSaver s3FileSaver, S3Client s3Client) {
        super(s3FileEntity);

        this.s3FileSaver = s3FileSaver;
        this.s3Client = s3Client;
    }

    @Override
    public void saveFromStream(InputStream fileUploadingInputStream) {
        s3FileSaver.writeFromStream(fileUploadingInputStream, indexedFileEntity);
    }

    @Override
    public void cleanUpFailedUpload() {
        // Nothing to do here
    }

    @Override
    public void delete() {
        final DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                .bucket(indexedFileEntity.getBucket())
                .key(indexedFileEntity.getObjectKey())
                .build();

        s3Client.deleteObject(deleteObjectRequest);
    }
}
