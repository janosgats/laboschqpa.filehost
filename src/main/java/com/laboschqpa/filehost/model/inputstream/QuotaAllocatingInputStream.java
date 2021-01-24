package com.laboschqpa.filehost.model.inputstream;

import com.laboschqpa.filehost.entity.IndexedFileEntity;
import com.laboschqpa.filehost.service.IndexedFileQuotaAllocator;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

@RequiredArgsConstructor
public class QuotaAllocatingInputStream extends CountingInputStream {
    private final InputStream wrappedInputStream;
    private final IndexedFileEntity indexedFileEntity;
    private final IndexedFileQuotaAllocator indexedFileQuotaAllocator;
    private final Long approximateFileSize;

    private long countOfReadBytes = 0;
    private long allAllocatedBytes = 0;

    /**
     * NOT thread-safe implementation
     * @return Total amount of bytes read by this stream.
     */
    public long getCountOfReadBytes() {
        return countOfReadBytes;
    }

    private void proceedAllocation(long newlyReadBytes) {
        if (newlyReadBytes >= 0) {
            countOfReadBytes += newlyReadBytes;
            if (countOfReadBytes > allAllocatedBytes) {
                allAllocatedBytes = indexedFileQuotaAllocator.allocateQuota(indexedFileEntity, countOfReadBytes, approximateFileSize);
            }
        }
        //If value is negative, that indicates EOF, so we don't proceed allocation
    }

    @Override
    public int read() throws IOException {
        int readByte = wrappedInputStream.read();
        if (readByte != -1) {
            proceedAllocation(1);
        }

        return readByte;
    }

    @Override
    public int read(byte[] b) throws IOException {
        int reallyReadCount = wrappedInputStream.read(b);
        proceedAllocation(reallyReadCount);
        return reallyReadCount;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int reallyReadCount = wrappedInputStream.read(b, off, len);
        proceedAllocation(reallyReadCount);
        return reallyReadCount;
    }

    @Override
    public byte[] readAllBytes() throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public byte[] readNBytes(int len) throws IOException {
        byte[] readBytes = wrappedInputStream.readNBytes(len);
        proceedAllocation(readBytes.length);
        return readBytes;
    }

    @Override
    public int readNBytes(byte[] b, int off, int len) throws IOException {
        int reallyReadCount = wrappedInputStream.readNBytes(b, off, len);
        proceedAllocation(reallyReadCount);
        return reallyReadCount;
    }

    @Override
    public long skip(long n) throws IOException {
        long reallySkippedCount = wrappedInputStream.skip(n);
        proceedAllocation(reallySkippedCount);
        return reallySkippedCount;
    }

    @Override
    public int available() throws IOException {
        return wrappedInputStream.available();
    }

    @Override
    public void close() throws IOException {
        super.close();
        wrappedInputStream.close();
    }

    @Override
    public synchronized void mark(int readLimit) {
        super.mark(readLimit);
        wrappedInputStream.mark(readLimit);
    }

    @Override
    public synchronized void reset() throws IOException {
        super.reset();
        wrappedInputStream.reset();
    }

    @Override
    public boolean markSupported() {
        return wrappedInputStream.markSupported();
    }

    @Override
    public long transferTo(OutputStream out) throws IOException {
        throw new UnsupportedOperationException();
    }
}
