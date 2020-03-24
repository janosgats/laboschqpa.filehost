package com.laboschcst.filehost.api.controller;

import com.laboschcst.filehost.config.filter.StoredFileRequestWrapper;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.*;
import java.nio.file.Path;

@RestController
@RequestMapping("/api/file")
public class FileServingController {

    @Value("${filehost.storedfiles.basepath}")
    private String storedFilesBasePath;

    @GetMapping("/**")
    public void getTest(StoredFileRequestWrapper request,
                        OutputStream outputStream) throws IOException {
        File f = new File(Path.of(storedFilesBasePath, request.getStoredFileDto().getPath()).toString());
        IOUtils.copy(new FileInputStream(f), outputStream);
    }
}
