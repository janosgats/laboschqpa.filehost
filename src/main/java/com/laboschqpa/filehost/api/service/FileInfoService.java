package com.laboschqpa.filehost.api.service;

import com.laboschqpa.filehost.api.dto.GetIndexedFileInfoResponse;
import com.laboschqpa.filehost.repo.IndexedFileEntityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class FileInfoService {
    private final IndexedFileEntityRepository indexedFileEntityRepository;

    public List<GetIndexedFileInfoResponse> getIndexedFileInfo(List<Long> indexedFileIds) {
        final Set<Long> remainingIds = new HashSet<>(indexedFileIds);

        final List<GetIndexedFileInfoResponse> out
                = indexedFileEntityRepository.findOnlyFromIndexedFileTableByMultipleIds(indexedFileIds)
                .stream()
                .map((row) -> {
                    remainingIds.remove(row.getId());
                    return new GetIndexedFileInfoResponse(row);
                }).collect(Collectors.toList());

        for (Long id : remainingIds) {
            out.add(new GetIndexedFileInfoResponse(id, false));
        }

        return out;
    }

}
