package com.laboschqpa.filehost.model.file.factory;

import com.laboschqpa.filehost.entity.LocalDiskFileEntity;
import com.laboschqpa.filehost.enums.IndexedFileStatus;
import com.laboschqpa.filehost.model.file.LocalDiskFile;
import com.laboschqpa.filehost.model.upload.FileUploadRequest;
import com.laboschqpa.filehost.repo.LocalDiskFileEntityRepository;
import com.laboschqpa.filehost.service.LocalDiskFileSaver;
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

    private final LocalDiskFileUtils localDiskFileUtils;
    private final LocalDiskFileEntityRepository localDiskFileEntityRepository;
    private final LocalDiskFileSaver localDiskFileSaver;

    public LocalDiskFile fromFileUploadRequest(FileUploadRequest fileUploadRequest, String originalFileName) {
        LocalDiskFileEntity localDiskFileEntity = createStoredFileEntityForUploadedFile(fileUploadRequest, originalFileName);
        logger.trace("Created localDiskFileEntity for file upload: {}", localDiskFileEntity);

        return new LocalDiskFile(localDiskFileUtils, localDiskFileEntity, localDiskFileSaver, false);
    }

    private LocalDiskFileEntity createStoredFileEntityForUploadedFile(FileUploadRequest fileUploadRequest, String originalFileName) {
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

        return localDiskFileEntity;
    }
}
