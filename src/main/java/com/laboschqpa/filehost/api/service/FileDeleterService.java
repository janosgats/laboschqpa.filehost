

package com.laboschqpa.filehost.api.service;

import com.laboschqpa.filehost.annotation.ExceptionWrappedFileServingClass;
import com.laboschqpa.filehost.enums.IndexedFileStatus;
import com.laboschqpa.filehost.enums.apierrordescriptor.FileServingApiError;
import com.laboschqpa.filehost.exceptions.apierrordescriptor.FileServingException;
import com.laboschqpa.filehost.model.file.DeletableFile;
import com.laboschqpa.filehost.model.file.factory.DeletableFileFactory;
import com.laboschqpa.filehost.repo.IndexedFileEntityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

@Log4j2
@RequiredArgsConstructor
@Service
@ExceptionWrappedFileServingClass
public class FileDeleterService {
    private final DeletableFileFactory deletableFileFactory;
    private final IndexedFileEntityRepository indexedFileEntityRepository;

    public void deleteFile(Long fileIdToDelete) {
        DeletableFile deletableFile = deletableFileFactory.fromIndexedFileId(fileIdToDelete);
        log.debug("Deleting file: {}", fileIdToDelete);

        if (deletableFile.getStatus() == IndexedFileStatus.DELETED) {
            throw new FileServingException(FileServingApiError.FILE_STATUS_IS_ALREADY_DELETED,
                    "Status of file " + fileIdToDelete + " is already deleted!");
        }

        try {
            deletableFile.delete();
            deletableFile.getEntity().setStatus(IndexedFileStatus.DELETED);
        } catch (Throwable e) {
            log.error("Error while deleting file {}! - " + e.getMessage(), deletableFile.getEntity().getId(), e);
            deletableFile.getEntity().setStatus(IndexedFileStatus.FAILED_DURING_DELETION);
            throw e;
        } finally {
            indexedFileEntityRepository.save(deletableFile.getEntity());
        }
    }
}
