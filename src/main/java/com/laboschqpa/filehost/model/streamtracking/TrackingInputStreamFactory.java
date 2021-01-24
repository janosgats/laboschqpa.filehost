package com.laboschqpa.filehost.model.streamtracking;

import com.laboschqpa.filehost.model.inputstream.TrackingInputStream;
import com.laboschqpa.filehost.service.GlobalStreamTrackerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.InputStream;

@RequiredArgsConstructor
@Service
public class TrackingInputStreamFactory {

    private final GlobalStreamTrackerService globalStreamTrackerService;

    public TrackingInputStream createForFileUpload(InputStream wrappedInputStream) {
        return new TrackingInputStream(wrappedInputStream, globalStreamTrackerService.getStreamTrackerForAllFileUploads());
    }

    public TrackingInputStream createForFileDownload(InputStream wrappedInputStream) {
        return new TrackingInputStream(wrappedInputStream, globalStreamTrackerService.getStreamTrackerForAllFileDownloads());
    }
}
