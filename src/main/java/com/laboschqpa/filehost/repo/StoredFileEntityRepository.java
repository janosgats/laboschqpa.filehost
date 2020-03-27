package com.laboschqpa.filehost.repo;

import com.laboschqpa.filehost.entity.StoredFileEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StoredFileEntityRepository extends JpaRepository<StoredFileEntity, Long> {
}
