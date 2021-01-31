package com.laboschqpa.filehost.api.service;

import com.laboschqpa.filehost.annotation.ExceptionWrappedFileServingClass;
import com.laboschqpa.filehost.enums.apierrordescriptor.FileServingApiError;
import com.laboschqpa.filehost.exceptions.apierrordescriptor.FileServingException;
import com.laboschqpa.filehost.model.file.HttpServableFile;
import com.laboschqpa.filehost.model.file.factory.HttpServableFileFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;

@Log4j2
@RequiredArgsConstructor
@Service
@ExceptionWrappedFileServingClass
public class FileDownloaderService {

    private final HttpServableFileFactory httpServableFileFactory;

    public ResponseEntity<Resource> downloadFile(Long indexedFileId, HttpServletRequest request) {//TODO: Optimize browser file caching
        final HttpServableFile httpServableFile = httpServableFileFactory.from(indexedFileId);

        if (!httpServableFile.isAvailable()) {
            throw new FileServingException(FileServingApiError.FILE_IS_NOT_AVAILABLE,
                    "The requested file is not available for download. File status: " + httpServableFile.getStatus());
        }

        return httpServableFile.getDownloadResponseEntity(request);
    }
}
