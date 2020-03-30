package com.laboschqpa.filehost.entity;

import com.laboschqpa.filehost.enums.IndexedFileStatus;
import lombok.*;

import javax.persistence.*;
import java.time.Instant;


@Data
@NoArgsConstructor
@ToString(callSuper = true)
@Entity
@Table(name = "stored_file")
@DiscriminatorValue("1")
public class StoredFileEntity extends IndexedFileEntity {
    @Builder
    public StoredFileEntity(IndexedFileStatus status, final String path, final Long size, final Instant creationTime, final String eTag, final String originalFileName) {
        super(status);
        this.path = path;
        this.size = size;
        this.creationTime = creationTime;
        this.eTag = eTag;
        this.originalFileName = originalFileName;
    }

    @Column(name = "path")
    private String path;

    @JoinColumn(name = "size")
    private Long size;//Size in Bytes

    @Column(name = "creation_time", nullable = false)
    private Instant creationTime;

    @Column(name = "e_tag")
    private String eTag;

    @Column(name = "original_file_name")
    private String originalFileName;
}
