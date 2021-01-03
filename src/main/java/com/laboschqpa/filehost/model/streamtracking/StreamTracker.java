package com.laboschqpa.filehost.model.streamtracking;

import java.util.function.Function;

public interface StreamTracker {
    ReadOnlyStreamTracker readonly();

    void addToTrackedValue(long valueToAdd);

    String popAndFormatTrackingIntervalState();

    /**
     * Gets and resets tracking interval.
     *
     * @return elapsed time and trackedValue since last call to this function.
     */
    TrackingIntervalState popTrackingIntervalState();

    /**
     * @return trackedValueDifference since last call to {@link StreamTrackerImpl#popTrackingIntervalState}
     */
    long peekTrackedValueDifference();

    /**
     * @return elapsed time since last call to {@link StreamTrackerImpl#popTrackingIntervalState}
     */
    long peekElapsedTime();

    long getAbsoluteTrackedValue();

    String getName();

    Function<TrackingIntervalState, String> getTrackingIntervalStateFormatter();
}
