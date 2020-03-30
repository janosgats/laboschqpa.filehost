package com.laboschqpa.filehost.model.file.factory;

import com.laboschqpa.filehost.entity.StoredFileEntity;
import com.laboschqpa.filehost.enums.IndexedFileStatus;
import com.laboschqpa.filehost.model.file.SaveableFile;
import com.laboschqpa.filehost.model.file.StoredFile;
import com.laboschqpa.filehost.repo.StoredFileEntityRepository;
import com.laboschqpa.filehost.util.StoredFileUtils;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;

@RequiredArgsConstructor
@Service
public class SaveableFileFactory {
    private static final Logger logger = LoggerFactory.getLogger(SaveableFileFactory.class);

    private final StoredFileUtils storedFileUtils;
    private final StoredFileEntityRepository storedFileEntityRepository;

    public SaveableFile fromFileUploadStream(String originalFileName) {
        StoredFileEntity storedFileEntity = createStoredFileEntityForUploadedFile(originalFileName);
        logger.trace("Created storedFileEntity for file upload: {}", storedFileEntity);

        return new StoredFile(storedFileUtils, storedFileEntity, false);
    }

    private StoredFileEntity createStoredFileEntityForUploadedFile(String originalFileName) {
        StoredFileEntity storedFileEntity = StoredFileEntity.builder()
                .status(IndexedFileStatus.ADDED_TO_DATABASE_INDEX)
                .originalFileName(originalFileName)
                .creationTime(Instant.now())
                .eTag(String.valueOf(System.nanoTime()))
                .build();

        storedFileEntityRepository.save(storedFileEntity);//Saving the entity to get the file ID by AutoIncrement
        storedFileEntity.setPath(storedFileUtils.generateNewStoredFileEntityPath(storedFileEntity.getId()));
        storedFileEntityRepository.save(storedFileEntity);

        return storedFileEntity;
    }
}
