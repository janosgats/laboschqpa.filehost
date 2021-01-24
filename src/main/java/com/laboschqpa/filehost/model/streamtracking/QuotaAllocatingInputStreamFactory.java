package com.laboschqpa.filehost.model.streamtracking;

import com.laboschqpa.filehost.entity.IndexedFileEntity;
import com.laboschqpa.filehost.model.inputstream.QuotaAllocatingInputStream;
import com.laboschqpa.filehost.service.IndexedFileQuotaAllocator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.InputStream;

@RequiredArgsConstructor
@Service
public class QuotaAllocatingInputStreamFactory {
    private final IndexedFileQuotaAllocator indexedFileQuotaAllocator;

    public QuotaAllocatingInputStream from(InputStream inputStream, IndexedFileEntity indexedFileEntity, Long approximateFileSize) {
        return new QuotaAllocatingInputStream(inputStream, indexedFileEntity, approximateFileSize, indexedFileQuotaAllocator);
    }
}
