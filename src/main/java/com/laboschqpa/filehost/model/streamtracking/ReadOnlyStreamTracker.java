package com.laboschqpa.filehost.model.streamtracking;


import java.util.function.Function;

public class ReadOnlyStreamTracker implements StreamTracker {
    private final StreamTracker delegate;

    public ReadOnlyStreamTracker(StreamTracker delegate) {
        this.delegate = delegate;
    }

    public Function<TrackingIntervalState, String> getTrackingIntervalStateFormatter() {
        return delegate.getTrackingIntervalStateFormatter();
    }

    public long peekTrackedValueDifference() {
        return delegate.peekTrackedValueDifference();
    }

    public long peekElapsedTime() {
        return delegate.peekElapsedTime();
    }

    public long getAbsoluteTrackedValue() {
        return delegate.getAbsoluteTrackedValue();
    }

    @Override
    public String getName() {
        return delegate.getName();
    }

    @Override
    public ReadOnlyStreamTracker readonly() {
        return this;
    }

    @Override
    public void addToTrackedValue(long valueToAdd) {
        throw new UnsupportedOperationException("addToTrackedValue() called on a ReadOnlyStreamTracker");
    }

    @Override
    public String popAndFormatTrackingIntervalState() {
        throw new UnsupportedOperationException("popAndFormatTrackingIntervalState() called on a ReadOnlyStreamTracker");
    }

    @Override
    public TrackingIntervalState popTrackingIntervalState() {
        throw new UnsupportedOperationException("popTrackingIntervalState() called on a ReadOnlyStreamTracker");
    }

}
