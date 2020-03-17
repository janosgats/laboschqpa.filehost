package com.laboschcst.filehost.config.filter;

import com.laboschcst.filehost.api.dto.StoredFileDto;
import lombok.Getter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

@Getter
public class StoredFileServletRequestWrapper extends HttpServletRequestWrapper {
    private StoredFileDto storedFileDto;

    public StoredFileServletRequestWrapper(HttpServletRequest request, StoredFileDto storedFileDto) {
        super(request);
        this.storedFileDto = storedFileDto;
    }
}
