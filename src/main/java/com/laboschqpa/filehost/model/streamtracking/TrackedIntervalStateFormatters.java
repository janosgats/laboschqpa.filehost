package com.laboschqpa.filehost.model.streamtracking;

import java.text.DecimalFormat;
import java.text.NumberFormat;

public class TrackedIntervalStateFormatters {
    private static final NumberFormat trackingNumberFormat = new DecimalFormat("#0.00");
    private static final long COUNT_OF_BYTES_IN_A_KB = 1024L;
    private static final long COUNT_OF_BYTES_IN_A_MB = 1024L * COUNT_OF_BYTES_IN_A_KB;
    private static final long COUNT_OF_BYTES_IN_A_GB = 1024L * COUNT_OF_BYTES_IN_A_MB;
    private static final int COUNT_OF_MILLIS_IN_A_SEC = 1000;

    private TrackedIntervalStateFormatters() {
    }

    public static String formatAllGbPerSecSpeedMb(TrackingIntervalState trackingIntervalState) {
        String allReadInMb = trackingNumberFormat.format(trackingIntervalState.getAbsoluteTrackedValue() / (float) COUNT_OF_BYTES_IN_A_GB);
        String readingSpeedInMbPerSec = trackingNumberFormat.format(
                (trackingIntervalState.getTrackedValueDifference() / (float) COUNT_OF_BYTES_IN_A_MB)
                        / (trackingIntervalState.getElapsedMillis() / (float) COUNT_OF_MILLIS_IN_A_SEC)
        );

        return allReadInMb + "GB - " + readingSpeedInMbPerSec + "MB/s";
    }
}
