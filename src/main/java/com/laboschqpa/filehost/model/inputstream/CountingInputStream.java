package com.laboschqpa.filehost.model.inputstream;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

@Log4j2
@RequiredArgsConstructor
public class CountingInputStream extends InputStream implements CountingInputStreamInterface {
    private final InputStream wrappedInputStream;

    private long countOfReadBytes = 0;

    /**
     * NOT thread-safe implementation
     *
     * @return Total amount of bytes read by this stream.
     */
    @Override
    public long getCountOfReadBytes() {
        return countOfReadBytes;
    }

    @Override
    public InputStream getInputStream() {
        return this;
    }

    private void proceedStreamReadingStatsIfValueIsNotNegative(long newlyReadBytes) {
        if (newlyReadBytes >= 0) {
            countOfReadBytes += newlyReadBytes;
        }
        //If value is negative, that indicates EOF, so we don't proceed the stats
    }

    @Override
    public int read() throws IOException {
        int readByte = wrappedInputStream.read();
        proceedStreamReadingStatsIfValueIsNotNegative(1);

        return readByte;
    }

    @Override
    public int read(byte[] b) throws IOException {
        int reallyReadCount = wrappedInputStream.read(b);
        proceedStreamReadingStatsIfValueIsNotNegative(reallyReadCount);
        return reallyReadCount;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int reallyReadCount = wrappedInputStream.read(b, off, len);
        proceedStreamReadingStatsIfValueIsNotNegative(reallyReadCount);
        return reallyReadCount;
    }

    @Override
    public byte[] readAllBytes() throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public byte[] readNBytes(int len) throws IOException {
        byte[] readBytes = wrappedInputStream.readNBytes(len);
        proceedStreamReadingStatsIfValueIsNotNegative(readBytes.length);
        return readBytes;
    }

    @Override
    public int readNBytes(byte[] b, int off, int len) throws IOException {
        int reallyReadCount = wrappedInputStream.readNBytes(b, off, len);
        proceedStreamReadingStatsIfValueIsNotNegative(reallyReadCount);
        return reallyReadCount;
    }

    @Override
    public long skip(long n) throws IOException {
        long reallySkippedCount = wrappedInputStream.skip(n);
        proceedStreamReadingStatsIfValueIsNotNegative(reallySkippedCount);
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
