package com.laboschqpa.filehost.model.streamtracking;


import lombok.Getter;

import java.util.function.Function;

public class StreamTrackerImpl implements StreamTracker {
    @Getter
    private final String name;

    private final ReadOnlyStreamTracker readOnlyStreamTracker;

    private volatile long absoluteTrackedValue;
    private volatile long trackedValueDifference;
    private volatile long startMillisOfLastInterval;

    @Getter
    private final Function<TrackingIntervalState, String> trackingIntervalStateFormatter;

    public StreamTrackerImpl(String name, Function<TrackingIntervalState, String> trackingIntervalStateFormatter) {
        this.readOnlyStreamTracker = new ReadOnlyStreamTracker(this);
        this.name = name;
        this.trackingIntervalStateFormatter = trackingIntervalStateFormatter;
        trackedValueDifference = 0;
        startMillisOfLastInterval = System.currentTimeMillis();
    }

    @Override
    public ReadOnlyStreamTracker readonly() {
        return readOnlyStreamTracker;
    }

    @Override
    public synchronized void addToTrackedValue(long valueToAdd) {
        trackedValueDifference += valueToAdd;
        absoluteTrackedValue += valueToAdd;
    }

    @Override
    public String popAndFormatTrackingIntervalState() {
        return trackingIntervalStateFormatter.apply(popTrackingIntervalState());
    }

    @Override
    public synchronized TrackingIntervalState popTrackingIntervalState() {
        TrackingIntervalState intervalState
                = new TrackingIntervalState(absoluteTrackedValue, trackedValueDifference, System.currentTimeMillis() - startMillisOfLastInterval);
        trackedValueDifference = 0;
        startMillisOfLastInterval = System.currentTimeMillis();

        return intervalState;
    }

    @Override
    public synchronized long peekTrackedValueDifference() {
        return trackedValueDifference;
    }

    @Override
    public synchronized long peekElapsedTime() {
        return System.currentTimeMillis() - startMillisOfLastInterval;
    }

    @Override
    public synchronized long getAbsoluteTrackedValue() {
        return absoluteTrackedValue;
    }

}
