package com.laboschqpa.filehost.model;

import com.laboschqpa.filehost.api.dto.IndexedFileServingRequestDto;
import com.laboschqpa.filehost.entity.IndexedFileEntity;
import com.laboschqpa.filehost.entity.StoredFileEntity;
import com.laboschqpa.filehost.exceptions.ContentNotFoundApiException;
import com.laboschqpa.filehost.exceptions.InvalidHttpRequestException;
import com.laboschqpa.filehost.repo.IndexedFileEntityRepository;
import com.laboschqpa.filehost.util.StoredFileUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@RequiredArgsConstructor
@Service
public class ServiceableFileFactory {
    private final IndexedFileEntityRepository indexedFileEntityRepository;
    private final StoredFileUtils storedFileUtils;

    public StoredFile from(StoredFileEntity storedFileEntity) {
        return new StoredFile(storedFileUtils, storedFileEntity);
    }

    public ServiceableFile from(IndexedFileServingRequestDto fileServingRequestDto) {
        Long indexedFileId = fileServingRequestDto.getIndexedFileId();
        Optional<IndexedFileEntity> indexedFileOptional = indexedFileEntityRepository.findById(indexedFileId);

        if (indexedFileOptional.isEmpty())
            throw new ContentNotFoundApiException("Cannot find indexed file with id: " + indexedFileId);

        IndexedFileEntity indexedFileEntity = indexedFileOptional.get();

        if (indexedFileEntity instanceof StoredFileEntity)
            return from((StoredFileEntity) indexedFileEntity);

        throw new InvalidHttpRequestException("Cannot create ServiceableFile from fileServingRequestDto: " + fileServingRequestDto.toString());
    }
}
