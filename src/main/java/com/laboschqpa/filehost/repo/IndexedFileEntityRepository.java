package com.laboschqpa.filehost.repo;

import com.laboschqpa.filehost.entity.IndexedFileEntity;
import com.laboschqpa.filehost.enums.IndexedFileStatus;
import com.laboschqpa.filehost.exceptions.ContentNotFoundApiException;
import com.laboschqpa.filehost.exceptions.fileserving.FileIsNotAvailableException;
import com.laboschqpa.filehost.repo.custom.ExtendedIndexedFileEntityRepository;
import com.laboschqpa.filehost.repo.dto.IndexedFileOnlyJpaDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface IndexedFileEntityRepository extends JpaRepository<IndexedFileEntity, Long>, ExtendedIndexedFileEntityRepository {

    @Query(value = "select id as id, dtype as dType, status as statusVal, owner_user_id as ownerUserId, owner_team_id as ownerTeamId, creation_time as creationTime, mime_type as mimeType " +
            " from indexed_file " +
            " where id = :id",
            nativeQuery = true)
    Optional<IndexedFileOnlyJpaDto> findOnlyFromIndexedFileTableById(@Param("id") Long id);

    default IndexedFileOnlyJpaDto getValidExistingAvailableIndexedFileOnlyJpaDto(Long indexedFileId) {
        Optional<IndexedFileOnlyJpaDto> indexedFileOnlyJpaDtoOptional = this.findOnlyFromIndexedFileTableById(indexedFileId);

        if (indexedFileOnlyJpaDtoOptional.isEmpty()) {
            throw new ContentNotFoundApiException("File with id "
                    + indexedFileId + " does not exist!");
        }
        IndexedFileOnlyJpaDto indexedFileOnlyJpaDto = indexedFileOnlyJpaDtoOptional.get();
        if (indexedFileOnlyJpaDto.getStatus() != IndexedFileStatus.AVAILABLE) {
            throw new FileIsNotAvailableException("File with id "
                    + indexedFileId + " is found, but it's not available!");
        }
        return indexedFileOnlyJpaDto;
    }
}
