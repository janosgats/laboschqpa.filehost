package com.laboschqpa.filehost.model.upload;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class FileUploadRequest {
    private long loggedInUserId;
    private long loggedInUserTeamId;
}
