package com.laboschqpa.filehost.model.file;

import com.laboschqpa.filehost.enums.S3Provider;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class S3ObjectSpecifier {
    private S3Provider provider;
    private String bucket;
    private String key;
}
