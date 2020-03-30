package com.laboschqpa.filehost.model.streaming;

import com.laboschqpa.filehost.exceptions.fileserving.StreamLengthLimitExceededException;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.text.NumberFormat;

@Data
@RequiredArgsConstructor
public class ReadTrackingInputStream extends InputStream {
    Logger logger = LoggerFactory.getLogger(ReadTrackingInputStream.class);
    private static final NumberFormat trackingNumberFormat = new DecimalFormat("#0.00");
    private static final long COUNT_OF_BYTES_IN_A_MB = 1024L * 1024L;
    private static final long READ_TRACKING_CALCULATION_BYTE_INTERVAL = 20 * COUNT_OF_BYTES_IN_A_MB;

    private final InputStream wrappedInputStream;

    private long byteReadCountInLastReadTrackingCalculationInterval = 0;
    private long countOfReadBytes = 0;

    /**
     * Stream length limit in bytes.<br>
     * {@link #read()} throws a {@link StreamLengthLimitExceededException} when {@link #countOfReadBytes} exceeds this limit.
     */
    private long limit = Long.MAX_VALUE;

    private long lastTimeMillisForSpeedCalculation = -1;

    @Override
    public int read() throws IOException {
        int readByte = wrappedInputStream.read();
        if (readByte != -1) {
            proceedStreamReadingStats(1);
        }

        return readByte;
    }

    @Override
    public int read(byte[] b) throws IOException {
        int reallyReadCount = wrappedInputStream.read(b);
        proceedStreamReadingStats(reallyReadCount);
        return reallyReadCount;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int reallyReadCount = wrappedInputStream.read(b, off, len);
        proceedStreamReadingStats(reallyReadCount);
        return reallyReadCount;
    }

    @Override
    public byte[] readAllBytes() throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public byte[] readNBytes(int len) throws IOException {
        byte[] readBytes = wrappedInputStream.readNBytes(len);
        proceedStreamReadingStats(readBytes.length);
        return readBytes;
    }

    @Override
    public int readNBytes(byte[] b, int off, int len) throws IOException {
        int reallyReadCount = wrappedInputStream.readNBytes(b, off, len);
        proceedStreamReadingStats(reallyReadCount);
        return reallyReadCount;
    }

    @Override
    public long skip(long n) throws IOException {
        long reallySkippedCount = wrappedInputStream.skip(n);
        proceedStreamReadingStats(reallySkippedCount);
        return reallySkippedCount;
    }

    @Override
    public void skipNBytes(long n) throws IOException {
        wrappedInputStream.skipNBytes(n);
        proceedStreamReadingStats(n);
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
    public synchronized void mark(int readlimit) {
        super.mark(readlimit);
        wrappedInputStream.mark(readlimit);
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

    private void proceedStreamReadingStats(long newlyReadBytes) {
        assertLengthLimit(countOfReadBytes + newlyReadBytes);

        if (lastTimeMillisForSpeedCalculation == -1)
            lastTimeMillisForSpeedCalculation = System.currentTimeMillis();

        byteReadCountInLastReadTrackingCalculationInterval += newlyReadBytes;

        if (byteReadCountInLastReadTrackingCalculationInterval > READ_TRACKING_CALCULATION_BYTE_INTERVAL) {
            calculateStreamReadingStatsOfLastInterval();
        }

        countOfReadBytes += newlyReadBytes;
    }

    private void calculateStreamReadingStatsOfLastInterval() {
        long currentTimeMillis = System.currentTimeMillis();
        float elapsedTimeInSec = (currentTimeMillis - lastTimeMillisForSpeedCalculation) / (float) 1000;

        String allReadDataInMb = trackingNumberFormat.format(countOfReadBytes / (float) COUNT_OF_BYTES_IN_A_MB);
        String readingSpeedInMb = trackingNumberFormat.format((byteReadCountInLastReadTrackingCalculationInterval / elapsedTimeInSec) / COUNT_OF_BYTES_IN_A_MB);

        logger.trace("Read tracking: {}MB - {}MB/s", allReadDataInMb, readingSpeedInMb);

        lastTimeMillisForSpeedCalculation = currentTimeMillis;
        byteReadCountInLastReadTrackingCalculationInterval = 0;
    }

    private void assertLengthLimit(long countToCompareToLimit) {
        if (countToCompareToLimit > limit) {
            throw new StreamLengthLimitExceededException("Stream length limit (" + limit + ") exceeded!");
        }
    }
}
