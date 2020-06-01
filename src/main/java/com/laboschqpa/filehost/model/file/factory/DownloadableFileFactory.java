package com.laboschqpa.filehost.model.file.factory;

import com.laboschqpa.filehost.config.filter.WrappedExternalFileServingRequestDto;
import com.laboschqpa.filehost.entity.IndexedFileEntity;
import com.laboschqpa.filehost.entity.StoredFileEntity;
import com.laboschqpa.filehost.exceptions.ContentNotFoundApiException;
import com.laboschqpa.filehost.exceptions.InvalidHttpRequestException;
import com.laboschqpa.filehost.model.file.DownloadableFile;
import com.laboschqpa.filehost.model.file.StoredFile;
import com.laboschqpa.filehost.repo.IndexedFileEntityRepository;
import com.laboschqpa.filehost.util.StoredFileUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@RequiredArgsConstructor
@Service
public class DownloadableFileFactory {
    private final IndexedFileEntityRepository indexedFileEntityRepository;
    private final StoredFileUtils storedFileUtils;

    public StoredFile from(StoredFileEntity storedFileEntity) {
        return new StoredFile(storedFileUtils, storedFileEntity, null, null);
    }

    public DownloadableFile from(WrappedExternalFileServingRequestDto wrappedExternalFileServingRequestDto) {
        Long indexedFileId = wrappedExternalFileServingRequestDto.getIndexedFileId();
        Optional<IndexedFileEntity> indexedFileOptional = indexedFileEntityRepository.findById(indexedFileId);

        if (indexedFileOptional.isEmpty())
            throw new ContentNotFoundApiException("Cannot find indexed file with id: " + indexedFileId);

        IndexedFileEntity indexedFileEntity = indexedFileOptional.get();

        if (indexedFileEntity instanceof StoredFileEntity)
            return from((StoredFileEntity) indexedFileEntity);

        throw new InvalidHttpRequestException("Cannot create ServiceableFile from DownloadableFile: " + wrappedExternalFileServingRequestDto.toString());
    }
}
