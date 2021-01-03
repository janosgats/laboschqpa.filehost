package com.laboschqpa.filehost.model.streamtracking;

import lombok.Getter;
import lombok.ToString;
import org.springframework.context.ApplicationEvent;

@Getter
@ToString
public class TrackingIntervalStatePoppedEvent extends ApplicationEvent {
    private final ReadOnlyStreamTracker streamTracker;
    private final TrackingIntervalState trackingIntervalState;

    public TrackingIntervalStatePoppedEvent(Object source, ReadOnlyStreamTracker streamTracker, TrackingIntervalState trackingIntervalState) {
        super(source);
        this.streamTracker = streamTracker;
        this.trackingIntervalState = trackingIntervalState;
    }
}
