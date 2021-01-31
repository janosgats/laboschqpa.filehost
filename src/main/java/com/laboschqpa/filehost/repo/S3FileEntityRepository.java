package com.laboschqpa.filehost.repo;

import com.laboschqpa.filehost.entity.S3FileEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface S3FileEntityRepository extends JpaRepository<S3FileEntity, Long> {
}
