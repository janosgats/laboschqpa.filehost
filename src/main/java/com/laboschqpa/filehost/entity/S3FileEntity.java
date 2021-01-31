package com.laboschqpa.filehost.entity;

import com.laboschqpa.filehost.enums.S3Provider;
import com.laboschqpa.filehost.enums.attributeconverter.S3ProviderAttributeConverter;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import javax.persistence.*;
import java.time.Instant;


@SuperBuilder
@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@ToString(callSuper = true)
@Entity
@Table(name = "s3_file")
@DiscriminatorValue("2")
public class S3FileEntity extends IndexedFileEntity {
    @Convert(converter = S3ProviderAttributeConverter.class)
    @Column(name = "s3_provider", nullable = false)
    S3Provider s3Provider;

    @Column(name = "bucket", nullable = false)
    private String bucket;

    @Column(name = "object_key", nullable = false)
    private String objectKey;

    @Column(name = "cached_presigned_url")
    private String cachedPresignedUrl;

    @Column(name = "presigned_url_expiration")
    private Instant presignedUrlExpiration;
}
