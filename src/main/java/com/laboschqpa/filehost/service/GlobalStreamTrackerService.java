package com.laboschqpa.filehost.service;

import com.laboschqpa.filehost.model.streamtracking.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;

@Log4j2
@RequiredArgsConstructor
@Service
public class GlobalStreamTrackerService implements Runnable {
    private final ApplicationEventPublisher applicationEventPublisher;

    private final List<StreamTracker> streamTrackers = new ArrayList<>();

    private StreamTracker allFileUploadsTracker;
    private StreamTracker allFileDownloadsTracker;

    @PostConstruct
    private void setUp() {
        allFileUploadsTracker = new StreamTrackerImpl("AllFileUploads", TrackedIntervalStateFormatters::formatAllGbPerSecSpeedMb);
        registerNewTracker(allFileUploadsTracker);
        allFileDownloadsTracker = new StreamTrackerImpl("AllFileDownloads", TrackedIntervalStateFormatters::formatAllGbPerSecSpeedMb);
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
                log.trace("{}: {}", streamTracker.getName(), streamTracker.getTrackingIntervalStateFormatter().apply(poppedIntervalState));

                applicationEventPublisher.publishEvent(new TrackingIntervalStatePoppedEvent(this, streamTracker.readonly(), poppedIntervalState));
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
