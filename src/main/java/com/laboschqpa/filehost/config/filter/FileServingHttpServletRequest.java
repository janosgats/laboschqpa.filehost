package com.laboschqpa.filehost.config.filter;

import com.laboschqpa.filehost.api.dto.IndexedFileServingRequestDto;
import lombok.Getter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

@Getter
public class FileServingHttpServletRequest extends HttpServletRequestWrapper {
    private IndexedFileServingRequestDto indexedFileServingRequestDto;

    public FileServingHttpServletRequest(HttpServletRequest request, IndexedFileServingRequestDto indexedFileServingRequestDto) {
        super(request);
        this.indexedFileServingRequestDto = indexedFileServingRequestDto;
    }
}
