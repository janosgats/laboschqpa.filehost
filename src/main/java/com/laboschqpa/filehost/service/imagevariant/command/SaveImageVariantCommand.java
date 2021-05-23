package com.laboschqpa.filehost.service.imagevariant.command;

import lombok.Data;
import lombok.RequiredArgsConstructor;

import javax.servlet.http.HttpServletRequest;

@Data
@RequiredArgsConstructor
public class SaveImageVariantCommand {
    private HttpServletRequest httpServletRequest;

    private Long jobId;
}
