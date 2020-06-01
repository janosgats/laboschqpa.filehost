package com.laboschqpa.filehost.model.file.factory;

import com.laboschqpa.filehost.config.filter.WrappedExternalFileServingRequestDto;
import com.laboschqpa.filehost.entity.StoredFileEntity;
import com.laboschqpa.filehost.enums.IndexedFileStatus;
import com.laboschqpa.filehost.model.file.StoredFile;
import com.laboschqpa.filehost.repo.StoredFileEntityRepository;
import com.laboschqpa.filehost.service.QuotaAllocatingStoredFileSaver;
import com.laboschqpa.filehost.util.StoredFileUtils;
import lombok.RequiredArgsConstructor;
import org.apache.tika.detect.Detector;
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
    private final Detector tikaDetector;

    public StoredFile fromFileUploadRequest(WrappedExternalFileServingRequestDto wrappedExternalFileServingRequestDto, String originalFileName) {
        StoredFileEntity storedFileEntity = createStoredFileEntityForUploadedFile(wrappedExternalFileServingRequestDto, originalFileName);
        logger.trace("Created storedFileEntity for file upload: {}", storedFileEntity);

        return new StoredFile(storedFileUtils, storedFileEntity, quotaAllocatingStoredFileSaver, tikaDetector, false);
    }

    private StoredFileEntity createStoredFileEntityForUploadedFile(WrappedExternalFileServingRequestDto wrappedExternalFileServingRequestDto, String originalFileName) {
        StoredFileEntity storedFileEntity = StoredFileEntity.builder()
                .status(IndexedFileStatus.ADDED_TO_DATABASE_INDEX)
                .originalFileName(originalFileName)
                .ownerUserId(wrappedExternalFileServingRequestDto.getLoggedInUserId())
                .ownerTeamId(wrappedExternalFileServingRequestDto.getLoggedInUserTeamId())
                .creationTime(Instant.now())
                .eTag(String.valueOf(System.nanoTime()))
                .build();

        storedFileEntityRepository.save(storedFileEntity);//Saving the entity to get the file ID by AutoIncrement
        storedFileEntity.setPath(storedFileUtils.generateNewStoredFileEntityPath(storedFileEntity.getId()));
        storedFileEntityRepository.save(storedFileEntity);

        return storedFileEntity;
    }
}
