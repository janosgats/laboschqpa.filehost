package com.laboschqpa.filehost.entity;

import com.laboschqpa.filehost.enums.IndexedFileStatus;
import com.laboschqpa.filehost.enums.attributeconverter.IndexedFileStatusAttributeConverter;
import lombok.*;

import javax.persistence.*;
import java.time.Instant;

/**
 * Every file/stream that's served by the FileHost webservice has to be indexed by adding it to this entity.
 * Files that are stored 'locally' by the webservice are instances of the "StoredFileEntity" subclass.
 */
@NoArgsConstructor
@Data
@ToString
@Entity
@Table(name = "indexed_file",
        indexes = {
                @Index(columnList = "owner_team_id, creation_time", name = "owner_team__creation_time"),
                @Index(columnList = "owner_user_id, owner_team_id, creation_time", name = "owner_user__owner_team__creation_time"),
                @Index(columnList = "status, owner_team_id, creation_time", name = "status__owner_team__creation_time"),
                @Index(columnList = "status, owner_user_id, owner_team_id, creation_time", name = "status__owner_user__owner_team__creation_time"),
                @Index(columnList = "mime_type, creation_time", name = "mime_type__creation_time"),
                @Index(columnList = "creation_time", name = "creation_time")
        }
)
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(
        discriminatorType = DiscriminatorType.INTEGER,
        name = "dtype",
        columnDefinition = "TINYINT"
)
public class IndexedFileEntity {
    public IndexedFileEntity(IndexedFileStatus status, Long ownerUserId, Long ownerTeamId, Instant creationTime) {
        this.status = status;
        this.ownerUserId = ownerUserId;
        this.ownerTeamId = ownerTeamId;
        this.creationTime = creationTime;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Convert(converter = IndexedFileStatusAttributeConverter.class)
    @Column(name = "status", nullable = false)
    private IndexedFileStatus status;

    @Column(name = "owner_user_id", nullable = false)
    private Long ownerUserId;

    @Column(name = "owner_team_id", nullable = false)
    private Long ownerTeamId;

    @Column(name = "creation_time", nullable = false)
    private Instant creationTime;

    @Column(name = "mime_type")
    private String mimeType;
}
