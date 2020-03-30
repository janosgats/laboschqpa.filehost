package com.laboschqpa.filehost.util;

import com.laboschqpa.filehost.entity.StoredFileEntity;
import com.laboschqpa.filehost.exceptions.fileserving.FileSavingException;
import com.laboschqpa.filehost.exceptions.fileserving.FileServingException;
import com.laboschqpa.filehost.exceptions.fileserving.InvalidStoredFileException;
import com.laboschqpa.filehost.repo.StoredFileEntityRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.file.Path;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Objects;

@RequiredArgsConstructor
@Service
public class StoredFileUtils {
    private static final String ACTIVE_MOUNT_NAME = "mnt1";

    @Value("${filehost.storedfiles.basepath}")
    private String storedFilesBasePath;

    @Value("${filehost.upload.filemaxsize}")
    private Long uploadFileMaxSize;

    @Value("${filehost.upload.filesavingbuffersize}")
    private Integer uploadFileSavingBufferSize;

    private final StoredFileEntityRepository storedFileEntityRepository;

    public String getFullPathFromStoredFileEntityPath(String storedFileEntityPath) {
        if (storedFileEntityPath == null || storedFileEntityPath.isBlank()) {
            throw new InvalidStoredFileException("storedFileEntityPath is null or blank!");
        }
        return Path.of(storedFilesBasePath, storedFileEntityPath).toString();
    }

    public String generateNewStoredFileEntityPath(Long storedFileEntityId) {
        Objects.requireNonNull(storedFileEntityId, "storedFileEntityId cannot be null when generation new file path!");
        ZonedDateTime zonedDateTime = ZonedDateTime.now(ZoneId.of("UTC"));
        return Path.of(
                ACTIVE_MOUNT_NAME,
                String.valueOf(zonedDateTime.getYear()),
                String.valueOf(zonedDateTime.getMonthValue()),
                String.valueOf(zonedDateTime.getDayOfMonth()),
                "f_" + storedFileEntityId + ".sf"
        ).toString();
    }


    public void writeWholeStreamToFile(InputStream streamToWriteToFile, File file) {
        handleDirectoryStructureBeforeWritingToFile(file);

        try (BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(
                new FileOutputStream(file, false),
                getUploadFileSavingBufferSize()
        )) {
            IOUtils.copy(streamToWriteToFile, bufferedOutputStream, getUploadFileSavingBufferSize());
            bufferedOutputStream.flush();
        } catch (Exception e) {
            throw new FileServingException("Cannot write stream to file!", e);
        }
    }

    private void handleDirectoryStructureBeforeWritingToFile(File file) {
        if (!file.getParentFile().exists()) {
            if (!file.getParentFile().mkdirs()) {
                throw new FileSavingException("Couldn't create containing directory: " + file.getParentFile());
            }
        }
    }

    public Long getUploadFileMaxSize() {
        return uploadFileMaxSize;
    }

    public Integer getUploadFileSavingBufferSize() {
        return uploadFileSavingBufferSize;
    }

    public StoredFileEntity saveStoredFileEntity(StoredFileEntity storedFileEntity) {
        return storedFileEntityRepository.save(storedFileEntity);
    }
}
