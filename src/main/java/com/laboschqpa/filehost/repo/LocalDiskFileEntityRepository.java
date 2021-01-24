package com.laboschqpa.filehost.repo;

import com.laboschqpa.filehost.entity.LocalDiskFileEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LocalDiskFileEntityRepository extends JpaRepository<LocalDiskFileEntity, Long> {
}
