package com.laboschqpa.filehost.api.service;

import com.laboschqpa.filehost.config.annotation.ExceptionWrappedFileServingClass;
import com.laboschqpa.filehost.config.filter.WrappedFileServingHttpServletRequest;
import com.laboschqpa.filehost.exceptions.fileserving.*;
import com.laboschqpa.filehost.model.file.DownloadableFile;
import com.laboschqpa.filehost.model.file.factory.DownloadableFileFactory;
import com.laboschqpa.filehost.model.streamtracking.TrackingInputStream;
import com.laboschqpa.filehost.model.streamtracking.TrackingInputStreamFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Log4j2
@RequiredArgsConstructor
@Service
@ExceptionWrappedFileServingClass
public class FileDownloaderService {

    private final DownloadableFileFactory downloadableFileFactory;
    private final TrackingInputStreamFactory trackingInputStreamFactory;

    public ResponseEntity<Resource> downloadFile(WrappedFileServingHttpServletRequest request) {
        DownloadableFile downloadableFile = downloadableFileFactory.from(request.getWrappedFileServingRequestDto());

        if (downloadableFile.isAvailable()) {
            String ifNoneMatchHeaderValue = request.getHeader(HttpHeaders.IF_NONE_MATCH);
            if (ifNoneMatchHeaderValue != null
                    && !ifNoneMatchHeaderValue.isBlank()
                    && ifNoneMatchHeaderValue.equals(downloadableFile.getETag())) {
                log.trace("ETag matches. Returning 304 - Not modified");
                return new ResponseEntity<>(HttpStatus.NOT_MODIFIED);
            }

            HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            httpHeaders.setContentLength(downloadableFile.getSize());
            httpHeaders.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + downloadableFile.getOriginalFileName() + "\"");

            TrackingInputStream downloadTrackingStream = trackingInputStreamFactory.createForFileDownload(downloadableFile.getStream());
            return new ResponseEntity<>(new InputStreamResource(downloadTrackingStream), httpHeaders, HttpStatus.OK);
        } else {
            throw new FileIsNotAvailableException("The requested file is not available for download. File status: " + downloadableFile.getStatus());
        }
    }
}
