package com.laboschqpa.filehost.service;

import com.laboschqpa.filehost.model.streamtracking.StreamTracker;
import com.laboschqpa.filehost.model.streamtracking.TrackedIntervalStateFormatters;
import com.laboschqpa.filehost.model.streamtracking.TrackingIntervalState;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;

@Log4j2
@RequiredArgsConstructor
@Service
public class GlobalStreamTrackerService implements Runnable {
    private static final String METRIC_NAME_GLOBAL_STREAM_TRACKER_TRACKED_VALUE = "global_stream_tracker_tracked_value";
    private static final String TAG_NAME_STREAM_TRACKER_NAME = "trackerName";

    private final MeterRegistry meterRegistry;

    private final List<StreamTracker> streamTrackers = new ArrayList<>();

    private StreamTracker allFileUploadsTracker;
    private StreamTracker allFileDownloadsTracker;

    @PostConstruct
    private void setUp() {
        allFileUploadsTracker = new StreamTracker("AllFileUploads", TrackedIntervalStateFormatters::formatAllGbPerSecSpeedMb);
        registerNewTracker(allFileUploadsTracker);
        allFileDownloadsTracker = new StreamTracker("AllFileDownloads", TrackedIntervalStateFormatters::formatAllGbPerSecSpeedMb);
        registerNewTracker(allFileDownloadsTracker);
    }

    public void registerNewTracker(StreamTracker streamTracker) {
        synchronized (streamTrackers) {
            if (streamTrackers.contains(streamTracker))
                throw new UnsupportedOperationException("StreamTracker is already registered.");

            streamTrackers.add(streamTracker);
        }
        log.info("StreamTracker registered: {}", streamTracker.getName());
    }

    @Override
    public void run() {
        popRegisteredStreamTrackers();
    }

    private void popRegisteredStreamTrackers() {
        synchronized (streamTrackers) {
            for (StreamTracker streamTracker : streamTrackers) {
                final TrackingIntervalState poppedIntervalState = streamTracker.popTrackingIntervalState();

                meterRegistry.counter(METRIC_NAME_GLOBAL_STREAM_TRACKER_TRACKED_VALUE,
                        TAG_NAME_STREAM_TRACKER_NAME, streamTracker.getName()).increment(poppedIntervalState.getTrackedValueDifference());

                log.debug("{}: {}", streamTracker.getName(), streamTracker.getTrackingIntervalStateFormatter().apply(poppedIntervalState));
            }
        }
    }

    public StreamTracker getStreamTrackerForAllFileUploads() {
        return allFileUploadsTracker;
    }

    public StreamTracker getStreamTrackerForAllFileDownloads() {
        return allFileDownloadsTracker;
    }
}
