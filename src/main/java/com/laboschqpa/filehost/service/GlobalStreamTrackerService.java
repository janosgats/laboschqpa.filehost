package com.laboschqpa.filehost.service;

import com.laboschqpa.filehost.model.streamtracking.StreamTracker;
import com.laboschqpa.filehost.model.streamtracking.TrackedIntervalStateFormatters;
import com.laboschqpa.filehost.model.streamtracking.TrackingIntervalState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;


@Service
public class GlobalStreamTrackerService implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(GlobalStreamTrackerService.class);

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
        logger.trace("StreamTracker registered: {}", streamTracker.getName());
    }

    @Override
    public void run() {
        synchronized (streamTrackers) {
            for (StreamTracker streamTracker : streamTrackers) {
                logger.debug("{}: {}", streamTracker.getName(), streamTracker.popAndFormatTrackingIntervalState());
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
