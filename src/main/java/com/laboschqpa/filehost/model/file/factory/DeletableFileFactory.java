package com.laboschqpa.filehost.model.file.factory;

import com.laboschqpa.filehost.entity.IndexedFileEntity;
import com.laboschqpa.filehost.entity.LocalDiskFileEntity;
import com.laboschqpa.filehost.exceptions.InvalidHttpRequestException;
import com.laboschqpa.filehost.exceptions.apierrordescriptor.ContentNotFoundException;
import com.laboschqpa.filehost.model.file.DeletableFile;
import com.laboschqpa.filehost.model.file.LocalDiskFile;
import com.laboschqpa.filehost.repo.IndexedFileEntityRepository;
import com.laboschqpa.filehost.util.LocalDiskFileUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@RequiredArgsConstructor
@Service
public class DeletableFileFactory {
    private final IndexedFileEntityRepository indexedFileEntityRepository;
    private final LocalDiskFileUtils localDiskFileUtils;

    public LocalDiskFile from(LocalDiskFileEntity localDiskFileEntity) {
        return new LocalDiskFile(localDiskFileUtils, localDiskFileEntity, null, null, false);
    }

    public DeletableFile fromIndexedFileId(Long indexedFileId) {
        Optional<IndexedFileEntity> indexedFileOptional = indexedFileEntityRepository.findById(indexedFileId);

        if (indexedFileOptional.isEmpty())
            throw new ContentNotFoundException("Cannot find indexed file with id: " + indexedFileId);

        IndexedFileEntity indexedFileEntity = indexedFileOptional.get();

        if (indexedFileEntity instanceof LocalDiskFileEntity)
            return from((LocalDiskFileEntity) indexedFileEntity);

        throw new InvalidHttpRequestException("Cannot create DeletableFile from indexedFileId: " + indexedFileId);
    }
}
