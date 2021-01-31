package com.laboschqpa.filehost.model.file;

import com.laboschqpa.filehost.entity.LocalDiskFileEntity;
import com.laboschqpa.filehost.enums.apierrordescriptor.FileServingApiError;
import com.laboschqpa.filehost.exceptions.apierrordescriptor.FileServingException;
import com.laboschqpa.filehost.service.LocalDiskFileSaver;
import com.laboschqpa.filehost.util.IOExceptionUtils;
import com.laboschqpa.filehost.util.LocalDiskFileUtils;
import lombok.extern.log4j.Log4j2;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Objects;

@Log4j2
public class LocalDiskFile extends AbstractIndexedFile<LocalDiskFileEntity> implements DownloadableFile, DeletableFile, UploadableFile {
    private final LocalDiskFileUtils localDiskFileUtils;
    private final LocalDiskFileSaver localDiskFileSaver;

    private File file;
    private FileInputStream readingStream;

    public LocalDiskFile(LocalDiskFileUtils localDiskFileUtils, LocalDiskFileEntity localDiskFileEntity, LocalDiskFileSaver localDiskFileSaver) {
        this(localDiskFileUtils, localDiskFileEntity, localDiskFileSaver, true);
    }

    public LocalDiskFile(LocalDiskFileUtils localDiskFileUtils, LocalDiskFileEntity localDiskFileEntity, LocalDiskFileSaver localDiskFileSaver, boolean assertFileCurrentlyExists) {
        super(localDiskFileEntity);
        Objects.requireNonNull(localDiskFileUtils);

        this.localDiskFileUtils = localDiskFileUtils;
        this.localDiskFileSaver = localDiskFileSaver;

        file = new File(localDiskFileUtils.getFullPathFromStoredFileEntityPath(localDiskFileEntity.getPath()));

        if (assertFileCurrentlyExists) {
            if (!isFileCurrentlyExisting())
                throw new FileServingException(FileServingApiError.INVALID_STORED_FILE, "File from localDiskFileEntity is not a valid file: " + file.getAbsolutePath());
        }
    }

    @Override
    public void saveFromStream(InputStream fileUploadingInputStream) {
        localDiskFileSaver.writeFromStream(fileUploadingInputStream, file, indexedFileEntity);
    }

    @Override
    public void cleanUpFailedUpload() {
        String fullPath = localDiskFileUtils.getFullPathFromStoredFileEntityPath(indexedFileEntity.getPath());

        IOExceptionUtils.wrap(() -> java.nio.file.Files.deleteIfExists(Path.of(fullPath)),
                "IOException while deleting LocalDiskFile after failed upload: " + fullPath);

        log.trace("File {} cleaned up after failed upload.", indexedFileEntity.getId());
    }

    @Override
    public InputStream getStream() {
        if (!isFileCurrentlyExisting())
            throw new FileServingException(FileServingApiError.FILE_DOES_NOT_EXIST, "File does not exist currently!");

        if (!isAvailable())
            throw new FileServingException(FileServingApiError.FILE_IS_NOT_AVAILABLE, "The file is not available (yet)!");

        if (readingStream == null) {
            try {
                this.readingStream = new FileInputStream(file);
            } catch (Exception e) {
                throw new FileServingException(FileServingApiError.CANNOT_CREATE_FILE_READ_STREAM, "Cannot instantiate FileInputStream from StoredFile::file!", e);
            }
        }

        return readingStream;
    }

    @Override
    public String getETag() {
        return String.format("\"%s_%s\"",
                indexedFileEntity.getCreationTime().getEpochSecond(),
                indexedFileEntity.getStatus().getValue().toString()
        );
    }

    @Override
    public void delete() {
        if (!isFileCurrentlyExisting())
            throw new FileServingException(FileServingApiError.FILE_DOES_NOT_EXIST, "File does not exist currently!");

        IOExceptionUtils.wrap(() -> java.nio.file.Files.delete(Path.of(file.getAbsolutePath())),
                "Cannot delete file: " + file.getAbsolutePath());
    }

    private boolean isFileCurrentlyExisting() {
        return file != null && file.exists() && file.isFile();
    }
}
