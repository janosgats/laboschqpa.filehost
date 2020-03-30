package com.laboschqpa.filehost.model.streaming;

import com.laboschqpa.filehost.exceptions.fileserving.StreamLengthLimitExceededException;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.text.NumberFormat;

@Data
@RequiredArgsConstructor
public class ReadTrackingInputStream extends InputStream {
    Logger logger = LoggerFactory.getLogger(ReadTrackingInputStream.class);
    private static final NumberFormat numberFormat = new DecimalFormat("#0.00");
    private static final long COUNT_OF_BYTES_IN_A_MB = 1024L * 1024L;
    private static final long READ_TRACKING_CALCULATION_BYTE_INTERVAL = 20 * COUNT_OF_BYTES_IN_A_MB;

    private final InputStream wrappedInputStream;

    private long countOfReadBytes = 0;

    /**
     * Stream length limit in bytes.<br>
     * {@link #read()} throws a {@link StreamLengthLimitExceededException} when {@link #countOfReadBytes} exceeds this limit.
     */
    private long limit = Long.MAX_VALUE;

    private long lastTimeMillisForSpeedCalculation;

    @Override
    public int read() throws IOException {
        if (countOfReadBytes >= limit)
            throw new StreamLengthLimitExceededException("Stream length limit (" + limit + ") exceeded!");

        int readByte = wrappedInputStream.read();
        if (readByte != -1) {
            calculateStreamReadingStats();
            ++countOfReadBytes;
        }

        return readByte;
    }

    private void calculateStreamReadingStats() {
        if (countOfReadBytes % READ_TRACKING_CALCULATION_BYTE_INTERVAL == 0) {
            long currentTimeMillis = System.currentTimeMillis();
            float elapsedSec = (currentTimeMillis - lastTimeMillisForSpeedCalculation) / (float) 1000;

            if (countOfReadBytes > 0) {
                logger.trace("Read tracking: {}MB - {}MB/s",
                        numberFormat.format(countOfReadBytes / (float) COUNT_OF_BYTES_IN_A_MB),
                        numberFormat.format((READ_TRACKING_CALCULATION_BYTE_INTERVAL / elapsedSec) / COUNT_OF_BYTES_IN_A_MB)
                );
            }
            lastTimeMillisForSpeedCalculation = currentTimeMillis;
        }
    }
}
