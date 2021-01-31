package com.laboschqpa.filehost.model.file.factory;

import com.laboschqpa.filehost.entity.IndexedFileEntity;
import com.laboschqpa.filehost.entity.LocalDiskFileEntity;
import com.laboschqpa.filehost.entity.S3FileEntity;
import com.laboschqpa.filehost.enums.S3Provider;
import com.laboschqpa.filehost.exceptions.InvalidHttpRequestException;
import com.laboschqpa.filehost.exceptions.apierrordescriptor.ContentNotFoundException;
import com.laboschqpa.filehost.model.file.HttpServableFile;
import com.laboschqpa.filehost.model.file.LocalDiskFile;
import com.laboschqpa.filehost.model.file.S3File;
import com.laboschqpa.filehost.model.streamtracking.TrackingInputStreamFactory;
import com.laboschqpa.filehost.repo.IndexedFileEntityRepository;
import com.laboschqpa.filehost.repo.S3FileEntityRepository;
import com.laboschqpa.filehost.util.LocalDiskFileUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.util.Optional;

@RequiredArgsConstructor
@Service
public class HttpServableFileFactory {
    private final IndexedFileEntityRepository indexedFileEntityRepository;
    private final LocalDiskFileUtils localDiskFileUtils;
    private final TrackingInputStreamFactory trackingInputStreamFactory;
    private final S3Presigner s3Presigner;
    private final S3FileEntityRepository s3FileEntityRepository;

    public LocalDiskFile from(LocalDiskFileEntity localDiskFileEntity) {
        return new LocalDiskFile(localDiskFileUtils, localDiskFileEntity, null, trackingInputStreamFactory);
    }

    public S3File from(S3FileEntity s3FileEntity) {
        if (s3FileEntity.getS3Provider() != S3Provider.SCALE_WAY) {
            throw new UnsupportedOperationException("S3 provider not supported yet: " + s3FileEntity.getS3Provider());
        }
        return new S3File(s3FileEntity, null, null, s3Presigner, s3FileEntityRepository);
    }

    public HttpServableFile from(Long indexedFileId) {
        Optional<IndexedFileEntity> indexedFileOptional = indexedFileEntityRepository.findById(indexedFileId);

        if (indexedFileOptional.isEmpty())
            throw new ContentNotFoundException("Cannot find indexed file with id: " + indexedFileId);

        IndexedFileEntity indexedFileEntity = indexedFileOptional.get();

        if (indexedFileEntity instanceof LocalDiskFileEntity)
            return from((LocalDiskFileEntity) indexedFileEntity);
        if (indexedFileEntity instanceof S3FileEntity)
            return from((S3FileEntity) indexedFileEntity);

        throw new InvalidHttpRequestException("Cannot create DownloadableFile from indexedFileId: " + indexedFileId);
    }
}
