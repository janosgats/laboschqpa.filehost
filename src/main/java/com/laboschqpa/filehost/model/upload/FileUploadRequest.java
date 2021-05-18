package com.laboschqpa.filehost.model.upload;

import com.laboschqpa.filehost.enums.UploadType;
import com.laboschqpa.filehost.enums.UploadedFileType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class FileUploadRequest {
    private long loggedInUserId;
    private long loggedInUserTeamId;
    private UploadType uploadType;

    private UploadedFileType forcedFileType;
}
