package com.laboschqpa.filehost.model.streamtracking;


import lombok.Getter;

import java.util.function.Function;

public class StreamTracker {
    @Getter
    private final String name;

    private volatile long absoluteTrackedValue;
    private volatile long trackedValueDifference;
    private volatile long startMillisOfLastInterval;

    @Getter
    private final Function<TrackingIntervalState, String> trackingIntervalStateFormatter;

    public StreamTracker(String name, Function<TrackingIntervalState, String> trackingIntervalStateFormatter) {
        this.name = name;
        this.trackingIntervalStateFormatter = trackingIntervalStateFormatter;

        trackedValueDifference = 0;
        startMillisOfLastInterval = System.currentTimeMillis();
    }

    public synchronized void addToTrackedValue(long valueToAdd) {
        trackedValueDifference += valueToAdd;
        absoluteTrackedValue += valueToAdd;
    }

    public String popAndFormatTrackingIntervalState() {
        return trackingIntervalStateFormatter.apply(popTrackingIntervalState());
    }

    /**
     * Gets and resets tracking interval.
     *
     * @return elapsed time and trackedValue since last call to this function.
     */
    public synchronized TrackingIntervalState popTrackingIntervalState() {
        TrackingIntervalState intervalState
                = new TrackingIntervalState(absoluteTrackedValue, trackedValueDifference, System.currentTimeMillis() - startMillisOfLastInterval);
        trackedValueDifference = 0;
        startMillisOfLastInterval = System.currentTimeMillis();

        return intervalState;
    }

    /**
     * @return trackedValueDifference since last call to {@link StreamTracker#popTrackingIntervalState}
     */
    public synchronized long peekTrackedValue() {
        return trackedValueDifference;
    }

    /**
     * @return elapsed time since last call to {@link StreamTracker#popTrackingIntervalState}
     */
    public synchronized long peekElapsedTime() {
        return System.currentTimeMillis() - startMillisOfLastInterval;
    }

    public synchronized long getAbsoluteTrackedValue() {
        return absoluteTrackedValue;
    }

}
