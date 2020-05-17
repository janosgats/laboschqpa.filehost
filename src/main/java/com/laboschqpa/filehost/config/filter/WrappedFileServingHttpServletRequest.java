package com.laboschqpa.filehost.config.filter;

import lombok.Getter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

@Getter
public class WrappedFileServingHttpServletRequest extends HttpServletRequestWrapper {
    private WrappedFileServingRequestDto wrappedFileServingRequestDto;

    public WrappedFileServingHttpServletRequest(HttpServletRequest request, WrappedFileServingRequestDto wrappedFileServingRequestDto) {
        super(request);
        this.wrappedFileServingRequestDto = wrappedFileServingRequestDto;
    }
}
