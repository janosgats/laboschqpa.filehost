package com.laboschqpa.filehost.model;

import com.laboschqpa.filehost.api.dto.IndexedFileServingRequestDto;
import com.laboschqpa.filehost.entity.IndexedFile;
import com.laboschqpa.filehost.entity.StoredFileEntity;
import com.laboschqpa.filehost.exceptions.ContentNotFoundApiException;
import com.laboschqpa.filehost.exceptions.InvalidHttpRequestException;
import com.laboschqpa.filehost.repo.IndexedFileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Optional;

@RequiredArgsConstructor
@Service
public class ServiceableFileFactory {
    private final IndexedFileRepository indexedFileRepository;

    @Value("${filehost.storedfiles.basepath}")
    private String storedFilesBasePath;

    public StoredFile from(StoredFileEntity storedFileEntity) {
        return new StoredFile(storedFilesBasePath, storedFileEntity);
    }

    public ServiceableFile from(IndexedFileServingRequestDto fileServingRequestDto) {
        Long indexedFileId = fileServingRequestDto.getIndexedFileId();
        Optional<IndexedFile> indexedFileOptional = indexedFileRepository.findById(indexedFileId);

        if (indexedFileOptional.isEmpty())
            throw new ContentNotFoundApiException("Cannot find indexed file with id: " + indexedFileId);

        IndexedFile indexedFile = indexedFileOptional.get();

        if (indexedFile instanceof StoredFileEntity)
            return from((StoredFileEntity) indexedFile);
        else {
            //Possible instantiations for other subclasses of IndexedFile, in the future
        }

        throw new InvalidHttpRequestException("Cannot create ServiceableFile from fileServingRequestDto: " + fileServingRequestDto.toString());
    }
}
