package com.laboschqpa.filehost.repo;

import com.laboschqpa.filehost.entity.IndexedFile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IndexedFileRepository extends JpaRepository<IndexedFile, Long> {
}
