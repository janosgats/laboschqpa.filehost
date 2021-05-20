package com.laboschqpa.filehost.entity;

import com.laboschqpa.filehost.enums.IndexedFileStatus;
import com.laboschqpa.filehost.enums.UploadType;
import com.laboschqpa.filehost.enums.attributeconverter.IndexedFileStatusAttributeConverter;
import com.laboschqpa.filehost.enums.attributeconverter.UploadTypeAttributeConverter;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import javax.persistence.*;
import java.time.Instant;

/**
 * Every file/stream that's served by the FileHost webservice has to be indexed by adding it to this table.
 */
@SuperBuilder
@NoArgsConstructor
@Data
@ToString
@Entity
@Table(name = "indexed_file",
        indexes = {
                @Index(columnList = "owner_team_id, size, creation_time", name = "owner_team__size__creation_time"),
                @Index(columnList = "owner_user_id, owner_team_id, creation_time", name = "owner_user__owner_team__creation_time"),
                @Index(columnList = "status, owner_team_id, creation_time", name = "status__owner_team__creation_time"),
                @Index(columnList = "status, owner_user_id, owner_team_id, creation_time", name = "status__owner_user__owner_team__creation_time"),
                @Index(columnList = "mime_type, creation_time", name = "mime_type__creation_time"),
                @Index(columnList = "creation_time", name = "creation_time"),
                @Index(columnList = "size, owner_user_id, owner_team_id", name = "size__owner_user__owner_team"),
                @Index(columnList = "is_image, upload_type", name = "is_image__upload_type")
        }
)
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(
        discriminatorType = DiscriminatorType.INTEGER,
        name = "dtype",
        columnDefinition = "TINYINT"
)
public class IndexedFileEntity {
    public IndexedFileEntity(Long id) {
        this.id = id;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Convert(converter = IndexedFileStatusAttributeConverter.class)
    @Column(name = "status", nullable = false)
    private IndexedFileStatus status;

    @Convert(converter = UploadTypeAttributeConverter.class)
    @Column(name = "upload_type", nullable = false)
    private UploadType uploadType;

    @Column(name = "owner_user_id", nullable = false)
    private Long ownerUserId;

    @Column(name = "owner_team_id", nullable = false)
    private Long ownerTeamId;

    @Column(name = "creation_time", columnDefinition = "datetime", nullable = false)
    private Instant creationTime;

    @Column(name = "mime_type")
    private String mimeType;

    /**
     * Should be derived from MIME type. Used to speed up selects.
     * {@code Null} means "couldn't determine".
     */
    @Column(name = "is_image")
    private Boolean isImage;

    @JoinColumn(name = "size")
    private Long size;//Size in Bytes

    @Column(name = "name")
    private String name;
}
