package com.laboschqpa.filehost.entity;

import com.laboschqpa.filehost.enums.StoredFileStatus;
import com.laboschqpa.filehost.enums.attributeconverter.StoredFileStatusAttributeConverter;
import lombok.Data;

import javax.persistence.*;
import java.time.Instant;

@Data
@Entity
@Table(name = "stored_file")
@DiscriminatorValue("1")
public class StoredFileEntity extends IndexedFile {
    @Convert(converter = StoredFileStatusAttributeConverter.class)
    @Column(name = "status", nullable = false)
    private StoredFileStatus status;

    @Column(name = "path", nullable = false)
    private String path;//Generated as: "<mnt_name>/<YYYY>/<MM>/<DD>/file<id>.sf"

    @JoinColumn(name = "size", nullable = false)
    private Long size;//Size in Bytes

    @Column(name = "creation_time", nullable = false)
    private Instant creationTime;

    @Column(name = "e_tag")
    private String eTag;
}
