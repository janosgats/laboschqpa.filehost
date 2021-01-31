package com.laboschqpa.filehost.model.file.factory;

import com.laboschqpa.filehost.entity.IndexedFileEntity;
import com.laboschqpa.filehost.entity.LocalDiskFileEntity;
import com.laboschqpa.filehost.entity.S3FileEntity;
import com.laboschqpa.filehost.enums.S3Provider;
import com.laboschqpa.filehost.exceptions.InvalidHttpRequestException;
import com.laboschqpa.filehost.exceptions.apierrordescriptor.ContentNotFoundException;
import com.laboschqpa.filehost.model.file.DeletableFile;
import com.laboschqpa.filehost.model.file.LocalDiskFile;
import com.laboschqpa.filehost.model.file.S3File;
import com.laboschqpa.filehost.repo.IndexedFileEntityRepository;
import com.laboschqpa.filehost.util.LocalDiskFileUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;

import java.util.Optional;

@RequiredArgsConstructor
@Service
public class DeletableFileFactory {
    private final IndexedFileEntityRepository indexedFileEntityRepository;
    private final LocalDiskFileUtils localDiskFileUtils;
    private final S3Client s3Client;

    public LocalDiskFile from(LocalDiskFileEntity localDiskFileEntity) {
        return new LocalDiskFile(localDiskFileUtils, localDiskFileEntity, null, null, false);
    }

    public S3File from(S3FileEntity s3FileEntity) {
        if (s3FileEntity.getS3Provider() != S3Provider.SCALE_WAY) {
            throw new UnsupportedOperationException("S3 provider not supported yet: " + s3FileEntity.getS3Provider());
        }
        return new S3File(s3FileEntity, null, s3Client, null, null);
    }

    public DeletableFile fromIndexedFileId(Long indexedFileId) {
        Optional<IndexedFileEntity> indexedFileOptional = indexedFileEntityRepository.findById(indexedFileId);

        if (indexedFileOptional.isEmpty())
            throw new ContentNotFoundException("Cannot find indexed file with id: " + indexedFileId);

        final IndexedFileEntity indexedFileEntity = indexedFileOptional.get();

        if (indexedFileEntity instanceof LocalDiskFileEntity)
            return from((LocalDiskFileEntity) indexedFileEntity);
        if (indexedFileEntity instanceof S3FileEntity)
            return from((S3FileEntity) indexedFileEntity);

        throw new InvalidHttpRequestException("Cannot create DeletableFile from indexedFileId: " + indexedFileId);
    }
}
