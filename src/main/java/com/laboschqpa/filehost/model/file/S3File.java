package com.laboschqpa.filehost.model.file;

import com.laboschqpa.filehost.entity.S3FileEntity;
import com.laboschqpa.filehost.exceptions.NotImplementedException;
import com.laboschqpa.filehost.service.S3FileSaver;
import lombok.extern.log4j.Log4j2;

import java.io.InputStream;
import java.util.Objects;

@Log4j2
public class S3File extends AbstractIndexedFile<S3FileEntity> implements UploadableFile {
    private final S3FileSaver s3FileSaver;

    public S3File(S3FileEntity s3FileEntity, S3FileSaver s3FileSaver) {
        super(Objects.requireNonNull(s3FileEntity));

        this.s3FileSaver = s3FileSaver;
    }

    @Override
    public void saveFromStream(InputStream fileUploadingInputStream) {
        s3FileSaver.writeFromStream(fileUploadingInputStream, indexedFileEntity);
    }

    @Override
    public void cleanUpFailedUpload() {
        if (true)
            throw new NotImplementedException("cleanUpFailedUpload");

        log.trace("File {} cleaned up after failed upload.", indexedFileEntity.getId());
    }
}
