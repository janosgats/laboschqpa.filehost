package com.laboschqpa.filehost.model.inputstream;

import com.laboschqpa.filehost.exceptions.apierrordescriptor.StreamLengthLimitExceededException;
import com.laboschqpa.filehost.model.streamtracking.StreamTracker;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

@Log4j2
@RequiredArgsConstructor
public class TrackingInputStream extends CountingInputStream {

    /**
     * {@link StreamTracker#addToTrackedValue(long)} method is synchronized to be threadsafe,
     * so we only add values of the current stream to the tracker periodically. (To cause fewer thread synchronizations.)
     */
    private static final Long STREAM_TRACKER_SYNCHRONIZATION_BYTE_COUNT_INTERVAL = 1000000L;
    private long countOfReadBytesInCurrentInterval = 0;

    private final InputStream wrappedInputStream;
    private final StreamTracker streamTracker;

    private long countOfReadBytes = 0;

    /**
     * NOT thread-safe implementation
     * @return Total amount of bytes read by this stream.
     */
    public long getCountOfReadBytes() {
        return countOfReadBytes;
    }

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
        countOfReadBytesInCurrentInterval += newlyReadBytes;
        if (countOfReadBytesInCurrentInterval > STREAM_TRACKER_SYNCHRONIZATION_BYTE_COUNT_INTERVAL) {
            registerCountOfReadBytesInCurrentIntervalIntoStreamTracker();
        }

        countOfReadBytes += newlyReadBytes;
        assertLengthLimit(countOfReadBytes);
    }

    private void registerCountOfReadBytesInCurrentIntervalIntoStreamTracker() {
        streamTracker.addToTrackedValue(countOfReadBytesInCurrentInterval);
        countOfReadBytesInCurrentInterval = 0;

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
    public int available() throws IOException {
        return wrappedInputStream.available();
    }

    @Override
    public void close() throws IOException {
        registerCountOfReadBytesInCurrentIntervalIntoStreamTracker();
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
