package com.laboschqpa.filehost.listener;

import com.laboschqpa.filehost.model.streamtracking.TrackingIntervalStatePoppedEvent;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Log4j2
@RequiredArgsConstructor
@Component
public class MicrometerTrackingIntervalStatePoppedEventListener implements ApplicationListener<TrackingIntervalStatePoppedEvent> {
    private static final String METRIC_NAME_GLOBAL_STREAM_TRACKER_TRACKED_VALUE = "global_stream_tracker_tracked_value";
    private static final String TAG_NAME_STREAM_TRACKER_NAME = "trackerName";

    private final MeterRegistry meterRegistry;

    @Override
    public void onApplicationEvent(TrackingIntervalStatePoppedEvent event) {
        meterRegistry.counter(METRIC_NAME_GLOBAL_STREAM_TRACKER_TRACKED_VALUE,
                TAG_NAME_STREAM_TRACKER_NAME, event.getStreamTracker().getName())
                .increment(event.getTrackingIntervalState().getTrackedValueDifference());
    }
}