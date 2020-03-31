package com.laboschqpa.filehost.model.streamtracking;

import com.laboschqpa.filehost.exceptions.fileserving.StreamLengthLimitExceededException;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

@RequiredArgsConstructor
public class TrackingInputStream extends InputStream {
    private static final Logger logger = LoggerFactory.getLogger(TrackingInputStream.class);

    private final InputStream wrappedInputStream;
    private final StreamTracker streamTracker;

    @Getter
    private long countOfReadBytes = 0;

    /**
     * Stream length limit in bytes.<br>
     * {@link #read()} throws a {@link StreamLengthLimitExceededException} when {@link #countOfReadBytes} exceeds this limit.
     */
    @Getter
    @Setter
    private long limit = Long.MAX_VALUE;

    private void proceedStreamReadingStatsIfValueIsNotNegative(long newlyReadBytes) {
        if (newlyReadBytes >= 0) {
            proceedStreamReadingStats(newlyReadBytes);
        }
        //If value is negative, that indicates EOF, so we don't proceed with the stats
    }

    private void proceedStreamReadingStats(long newlyReadBytes) {
        assertLengthLimit(countOfReadBytes + newlyReadBytes);
        streamTracker.addToTrackedValue(newlyReadBytes);
        countOfReadBytes += newlyReadBytes;
    }

    private void assertLengthLimit(long countToCompareToLimit) {
        if (countToCompareToLimit > limit) {
            throw new StreamLengthLimitExceededException("Stream length limit (" + limit + ") exceeded!");
        }
    }

    @Override
    public int read() throws IOException {
        int readByte = wrappedInputStream.read();
        if (readByte != -1) {
            proceedStreamReadingStatsIfValueIsNotNegative(1);
        }

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
    public void skipNBytes(long n) throws IOException {
        wrappedInputStream.skipNBytes(n);
        proceedStreamReadingStatsIfValueIsNotNegative(n);
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
