package com.laboschqpa.filehost.repo;

import com.laboschqpa.filehost.entity.IndexedFileEntity;
import com.laboschqpa.filehost.repo.dto.IndexedFileOnlyJpaDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface IndexedFileEntityRepository extends JpaRepository<IndexedFileEntity, Long> {

    @Query(value = "select id as id, dtype as dType, status as statusVal, owner_user_id as ownerUserId, owner_team_id as ownerTeamId " +
            " from indexed_file " +
            " where id = :id",
            nativeQuery = true)
    Optional<IndexedFileOnlyJpaDto> findOnlyFromIndexedFileTableById(@Param("id") Long id);
}
