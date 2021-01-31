package com.laboschqpa.filehost.service;

import com.laboschqpa.filehost.entity.LocalDiskFileEntity;
import com.laboschqpa.filehost.enums.IndexedFileStatus;
import com.laboschqpa.filehost.enums.apierrordescriptor.UploadApiError;
import com.laboschqpa.filehost.exceptions.apierrordescriptor.QuotaExceededException;
import com.laboschqpa.filehost.exceptions.apierrordescriptor.StreamLengthLimitExceededException;
import com.laboschqpa.filehost.exceptions.apierrordescriptor.UploadException;
import com.laboschqpa.filehost.repo.LocalDiskFileEntityRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;

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
        } catch (QuotaExceededException | StreamLengthLimitExceededException e) {
            localDiskFileEntity.setStatus(IndexedFileStatus.ABORTED_BY_FILE_HOST);
            throw e;
        } catch (RuntimeException e) {
            localDiskFileEntity.setStatus(IndexedFileStatus.FAILED);
            throw e;
        } catch (IOException e) {
            localDiskFileEntity.setStatus(IndexedFileStatus.FAILED);
            throw new UploadException(UploadApiError.IO_EXCEPTION_WHILE_SAVING_STREAM, "Cannot write stream to file!", e);
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
