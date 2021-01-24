package com.laboschqpa.filehost.service;

import com.laboschqpa.filehost.entity.LocalDiskFileEntity;
import com.laboschqpa.filehost.enums.IndexedFileStatus;
import com.laboschqpa.filehost.enums.apierrordescriptor.UploadApiError;
import com.laboschqpa.filehost.exceptions.apierrordescriptor.QuotaExceededException;
import com.laboschqpa.filehost.exceptions.apierrordescriptor.UploadException;
import com.laboschqpa.filehost.repo.LocalDiskFileEntityRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

@RequiredArgsConstructor
@Service
public class DefaultLocalDiskFileSaver implements LocalDiskFileSaver {
    private final LocalDiskFileEntityRepository localDiskFileEntityRepository;

    @Value("${filehost.localdiskfile.upload.filesavingmaxbuffersize}")
    private Integer fileSavingMaxBufferSize;

    @Override
    public void writeFromStream(InputStream streamToWriteIntoFile, File targetFile, LocalDiskFileEntity localDiskFileEntity) {
        handleDirectoryStructureBeforeWritingToFile(targetFile);

        try (BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(
                new FileOutputStream(targetFile, false), fileSavingMaxBufferSize)
        ) {
            final byte[] copyBuffer = new byte[fileSavingMaxBufferSize];

            IOUtils.copyLarge(streamToWriteIntoFile, bufferedOutputStream, copyBuffer);
            bufferedOutputStream.flush();
            localDiskFileEntity.setStatus(IndexedFileStatus.UPLOAD_STREAM_SAVED);
        } catch (QuotaExceededException e) {
            localDiskFileEntity.setStatus(IndexedFileStatus.ABORTED_BY_FILE_HOST);
            throw e;
        } catch (Exception e) {
            localDiskFileEntity.setStatus(IndexedFileStatus.FAILED);
            throw new UploadException(UploadApiError.CANNOT_WRITE_STREAM_TO_FILE, "Cannot write stream to file!", e);
        } finally {
            localDiskFileEntityRepository.save(localDiskFileEntity);
        }
    }

    private void handleDirectoryStructureBeforeWritingToFile(File file) {
        if (!file.getParentFile().exists()) {
            if (!file.getParentFile().mkdirs()) {
                throw new UploadException(UploadApiError.ERROR_DURING_SAVING_FILE,
                        "Couldn't create containing directory: " + file.getParentFile());
            }
        }
    }
}
