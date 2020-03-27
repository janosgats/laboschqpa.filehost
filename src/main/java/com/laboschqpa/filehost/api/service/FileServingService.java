package com.laboschqpa.filehost.api.service;

import com.laboschqpa.filehost.config.annotation.ExceptionWrappedFileServingClass;
import com.laboschqpa.filehost.config.filter.FileServingHttpServletRequest;
import com.laboschqpa.filehost.exceptions.fileserving.FileIsNotAvailableException;
import com.laboschqpa.filehost.model.ServiceableFile;
import com.laboschqpa.filehost.model.ServiceableFileFactory;
import lombok.RequiredArgsConstructor;
import org.apache.commons.io.IOUtils;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.OutputStream;

@RequiredArgsConstructor
@Service
@ExceptionWrappedFileServingClass
public class FileServingService {
    private final ServiceableFileFactory serviceableFileFactory;

    public void getTest(FileServingHttpServletRequest request, OutputStream outputStream) throws IOException {
        ServiceableFile serviceableFile = serviceableFileFactory.from(request.getIndexedFileServingRequestDto());

        if (serviceableFile.isAvailable()) {
            IOUtils.copy(serviceableFile.getStream(), outputStream);
        } else {
            throw new FileIsNotAvailableException("The requested file is not available for download. It might be under postprocessing.");
        }
    }
}
