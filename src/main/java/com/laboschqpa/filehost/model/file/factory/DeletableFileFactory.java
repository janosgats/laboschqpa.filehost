package com.laboschqpa.filehost.model.file.factory;

import com.laboschqpa.filehost.entity.IndexedFileEntity;
import com.laboschqpa.filehost.entity.StoredFileEntity;
import com.laboschqpa.filehost.exceptions.apierrordescriptor.ContentNotFoundException;
import com.laboschqpa.filehost.exceptions.InvalidHttpRequestException;
import com.laboschqpa.filehost.model.file.DeletableFile;
import com.laboschqpa.filehost.model.file.StoredFile;
import com.laboschqpa.filehost.repo.IndexedFileEntityRepository;
import com.laboschqpa.filehost.util.StoredFileUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@RequiredArgsConstructor
@Service
public class DeletableFileFactory {
    private final IndexedFileEntityRepository indexedFileEntityRepository;
    private final StoredFileUtils storedFileUtils;

    public StoredFile from(StoredFileEntity storedFileEntity) {
        return new StoredFile(storedFileUtils, storedFileEntity, null, null, false);
    }

    public DeletableFile fromIndexedFileId(Long indexedFileId) {
        Optional<IndexedFileEntity> indexedFileOptional = indexedFileEntityRepository.findById(indexedFileId);

        if (indexedFileOptional.isEmpty())
            throw new ContentNotFoundException("Cannot find indexed file with id: " + indexedFileId);

        IndexedFileEntity indexedFileEntity = indexedFileOptional.get();

        if (indexedFileEntity instanceof StoredFileEntity)
            return from((StoredFileEntity) indexedFileEntity);

        throw new InvalidHttpRequestException("Cannot create DeletableFile from indexedFileId: " + indexedFileId);
    }
}
