package com.laboschqpa.filehost.repo;

import com.laboschqpa.filehost.entity.IndexedFileEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IndexedFileEntityRepository extends JpaRepository<IndexedFileEntity, Long> {
}
