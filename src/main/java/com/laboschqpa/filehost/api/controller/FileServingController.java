package com.laboschqpa.filehost.api.controller;

import com.laboschqpa.filehost.api.service.FileServingService;
import com.laboschqpa.filehost.config.filter.FileServingHttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/file")
public class FileServingController {
    private final FileServingService fileServingService;

    @GetMapping("/**")
    public void getTest(FileServingHttpServletRequest request,
                        OutputStream outputStream) throws IOException {
        fileServingService.getTest(request, outputStream);
    }
}
