package com.laboschqpa.filehost.model.file.factory;

import com.laboschqpa.filehost.config.filter.WrappedFileServingRequestDto;
import com.laboschqpa.filehost.entity.StoredFileEntity;
import com.laboschqpa.filehost.enums.IndexedFileStatus;
import com.laboschqpa.filehost.model.file.StoredFile;
import com.laboschqpa.filehost.repo.StoredFileEntityRepository;
import com.laboschqpa.filehost.service.QuotaAllocatingStoredFileSaver;
import com.laboschqpa.filehost.util.StoredFileUtils;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;

@RequiredArgsConstructor
@Service
public class UploadableFileFactory {
    private static final Logger logger = LoggerFactory.getLogger(UploadableFileFactory.class);

    private final StoredFileUtils storedFileUtils;
    private final StoredFileEntityRepository storedFileEntityRepository;
    private final QuotaAllocatingStoredFileSaver quotaAllocatingStoredFileSaver;

    public StoredFile fromFileUploadRequest(WrappedFileServingRequestDto wrappedFileServingRequestDto, String originalFileName) {
        StoredFileEntity storedFileEntity = createStoredFileEntityForUploadedFile(wrappedFileServingRequestDto, originalFileName);
        logger.trace("Created storedFileEntity for file upload: {}", storedFileEntity);

        return new StoredFile(storedFileUtils, storedFileEntity, quotaAllocatingStoredFileSaver, false);
    }

    private StoredFileEntity createStoredFileEntityForUploadedFile(WrappedFileServingRequestDto wrappedFileServingRequestDto, String originalFileName) {
        StoredFileEntity storedFileEntity = StoredFileEntity.builder()
                .status(IndexedFileStatus.ADDED_TO_DATABASE_INDEX)
                .originalFileName(originalFileName)
                .ownerUserId(wrappedFileServingRequestDto.getLoggedInUserId())
                .ownerTeamId(wrappedFileServingRequestDto.getLoggedInUserTeamId())
                .creationTime(Instant.now())
                .eTag(String.valueOf(System.nanoTime()))
                .build();

        storedFileEntityRepository.save(storedFileEntity);//Saving the entity to get the file ID by AutoIncrement
        storedFileEntity.setPath(storedFileUtils.generateNewStoredFileEntityPath(storedFileEntity.getId()));
        storedFileEntityRepository.save(storedFileEntity);

        return storedFileEntity;
    }
}
