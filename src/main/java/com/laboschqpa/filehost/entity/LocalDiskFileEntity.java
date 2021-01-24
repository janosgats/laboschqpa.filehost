package com.laboschqpa.filehost.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import javax.persistence.*;


@SuperBuilder
@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@ToString(callSuper = true)
@Entity
@Table(name = "local_disk_file",
        indexes = {
                @Index(columnList = "size", name = "size")
        }
)
@DiscriminatorValue("1")
public class LocalDiskFileEntity extends IndexedFileEntity {
    @Column(name = "path")
    private String path;
}
