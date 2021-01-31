package com.laboschqpa.filehost.model.file.factory;

import com.laboschqpa.filehost.config.S3FileConfigurationProperties;
import com.laboschqpa.filehost.entity.LocalDiskFileEntity;
import com.laboschqpa.filehost.entity.S3FileEntity;
import com.laboschqpa.filehost.enums.IndexedFileStatus;
import com.laboschqpa.filehost.enums.S3Provider;
import com.laboschqpa.filehost.model.file.LocalDiskFile;
import com.laboschqpa.filehost.model.file.S3File;
import com.laboschqpa.filehost.model.file.S3ObjectSpecifier;
import com.laboschqpa.filehost.model.file.UploadableFile;
import com.laboschqpa.filehost.model.upload.FileUploadRequest;
import com.laboschqpa.filehost.repo.LocalDiskFileEntityRepository;
import com.laboschqpa.filehost.repo.S3FileEntityRepository;
import com.laboschqpa.filehost.service.LocalDiskFileSaver;
import com.laboschqpa.filehost.service.S3FileSaver;
import com.laboschqpa.filehost.util.LocalDiskFileUtils;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;

@RequiredArgsConstructor
@Service
public class UploadableFileFactory {
    private static final Logger logger = LoggerFactory.getLogger(UploadableFileFactory.class);

    private final S3FileConfigurationProperties s3FileConfigurationProperties;
    private final LocalDiskFileEntityRepository localDiskFileEntityRepository;
    private final S3FileEntityRepository s3FileEntityRepository;

    private final LocalDiskFileUtils localDiskFileUtils;
    private final LocalDiskFileSaver localDiskFileSaver;
    private final S3FileSaver s3FileSaver;

    public UploadableFile fromFileUploadRequest(FileUploadRequest fileUploadRequest, String originalFileName) {
        final boolean useS3 = true; //TODO: Put some more sophisticated logic to decide on where to upload

        if (useS3) {
            S3FileEntity s3FileEntity = createS3FileEntityForUploadedFile(fileUploadRequest, originalFileName);
            return new S3File(s3FileEntity, s3FileSaver);
        } else {
            LocalDiskFileEntity localDiskFileEntity = createLocalDiskFileEntityForUploadedFile(fileUploadRequest, originalFileName);
            return new LocalDiskFile(localDiskFileUtils, localDiskFileEntity, localDiskFileSaver, false);
        }
    }

    private LocalDiskFileEntity createLocalDiskFileEntityForUploadedFile(FileUploadRequest fileUploadRequest, String originalFileName) {
        LocalDiskFileEntity localDiskFileEntity = LocalDiskFileEntity.builder()
                .status(IndexedFileStatus.ADDED_TO_DATABASE_INDEX)
                .originalFileName(originalFileName)
                .ownerUserId(fileUploadRequest.getLoggedInUserId())
                .ownerTeamId(fileUploadRequest.getLoggedInUserTeamId())
                .creationTime(Instant.now())
                .build();

        localDiskFileEntityRepository.save(localDiskFileEntity);//Saving the entity to get the file ID by AutoIncrement
        localDiskFileEntity.setPath(localDiskFileUtils.generateNewLocalDiskFileEntityPath(localDiskFileEntity.getId()));
        localDiskFileEntityRepository.save(localDiskFileEntity);
        logger.trace("Created LocalDiskFileEntity for file upload: {}", localDiskFileEntity);

        return localDiskFileEntity;
    }

    private S3FileEntity createS3FileEntityForUploadedFile(FileUploadRequest fileUploadRequest, String originalFileName) {
        S3FileEntity s3FileEntity = S3FileEntity.builder()
                .status(IndexedFileStatus.ADDED_TO_DATABASE_INDEX)
                .originalFileName(originalFileName)
                .ownerUserId(fileUploadRequest.getLoggedInUserId())
                .ownerTeamId(fileUploadRequest.getLoggedInUserTeamId())
                .creationTime(Instant.now())
                .build();

        s3FileEntity.setS3Provider(S3Provider.UNDECIDED);// Set to avoid non-nullable DB constraint violation at the below save
        s3FileEntity.setBucket("");// Set to avoid non-nullable DB constraint violation at the below save
        s3FileEntity.setObjectKey("");// Set to avoid non-nullable DB constraint violation at the below save

        s3FileEntityRepository.save(s3FileEntity);//Saving the entity to get the file ID by AutoIncrement

        S3ObjectSpecifier s3ObjectSpecifier = getS3ObjectSpecifierForUploadedFile(s3FileEntity.getId());
        s3FileEntity.setS3Provider(s3ObjectSpecifier.getProvider());
        s3FileEntity.setBucket(s3ObjectSpecifier.getBucket());
        s3FileEntity.setObjectKey(s3ObjectSpecifier.getKey());

        s3FileEntityRepository.save(s3FileEntity);
        logger.trace("Created S3FileEntity for file upload: {}", s3FileEntity);

        return s3FileEntity;
    }

    private S3ObjectSpecifier getS3ObjectSpecifierForUploadedFile(long indexedFileId) {
        return new S3ObjectSpecifier(
                s3FileConfigurationProperties.getS3Provider(),
                s3FileConfigurationProperties.getBucket(),
                s3FileConfigurationProperties.getObjectKeyPrefix() + indexedFileId
        );
    }
}
