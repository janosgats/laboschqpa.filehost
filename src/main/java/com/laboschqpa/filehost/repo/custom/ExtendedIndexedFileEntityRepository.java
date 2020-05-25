package com.laboschqpa.filehost.repo.custom;

import com.laboschqpa.filehost.repo.dto.IndexedFileOnlyJpaDto;

import java.util.Collection;
import java.util.List;

public interface ExtendedIndexedFileEntityRepository {

    List<IndexedFileOnlyJpaDto> findOnlyFromIndexedFileTableByMultipleIds(Collection<Long> ids);
}