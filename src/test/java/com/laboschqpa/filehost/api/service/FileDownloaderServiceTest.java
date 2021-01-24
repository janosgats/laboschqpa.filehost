package com.laboschqpa.filehost.api.service;

import com.laboschqpa.filehost.model.file.DownloadableFile;
import com.laboschqpa.filehost.model.file.factory.DownloadableFileFactory;
import com.laboschqpa.filehost.model.inputstream.TrackingInputStream;
import com.laboschqpa.filehost.model.streamtracking.TrackingInputStreamFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FileDownloaderServiceTest {
    @Mock
    DownloadableFileFactory downloadableFileFactory;
    @Mock
    TrackingInputStreamFactory trackingInputStreamFactory;

    @InjectMocks
    FileDownloaderService fileDownloaderService;

    @Test
    void downloadFile_success_noCache() {
        final Long fileId = 432L;
        final String eTag = "\"aouhsdfig\"";
        final DownloadableFile downloadableFile = mock(DownloadableFile.class);
        when(downloadableFile.isAvailable())
                .thenReturn(true);

        final MockHttpServletRequest request = new MockHttpServletRequest();

        when(downloadableFile.getETag())
                .thenReturn(eTag);
        when(downloadableFileFactory.from(fileId))
                .thenReturn(downloadableFile);
        when(trackingInputStreamFactory.createForFileDownload(any()))
                .thenReturn(mock(TrackingInputStream.class));

        final ResponseEntity<Resource> responseEntity = fileDownloaderService.downloadFile(fileId, request);

        assertEquals(200, responseEntity.getStatusCode().value());
        assertEquals(eTag, responseEntity.getHeaders().getETag());
        assertEquals("public, immutable, max-age=31536000", responseEntity.getHeaders().getCacheControl());
    }

    @Test
    void downloadFile_success_cache() {
        final Long fileId = 432L;
        final String eTag = "\"aouhsdfig\"";
        final DownloadableFile downloadableFile = mock(DownloadableFile.class);
        when(downloadableFile.isAvailable())
                .thenReturn(true);

        final MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.IF_NONE_MATCH, eTag);

        when(downloadableFile.getETag())
                .thenReturn(eTag);
        when(downloadableFileFactory.from(fileId))
                .thenReturn(downloadableFile);

        ResponseEntity<Resource> responseEntity = fileDownloaderService.downloadFile(fileId, request);

        assertEquals(304, responseEntity.getStatusCode().value());

        verifyNoInteractions(trackingInputStreamFactory);
    }
}