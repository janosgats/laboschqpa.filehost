package com.laboschqpa.filehost.entity;

import com.laboschqpa.filehost.enums.IndexedFileStatus;
import com.laboschqpa.filehost.enums.attributeconverter.IndexedFileStatusAttributeConverter;
import lombok.*;

import javax.persistence.*;

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
                @Index(columnList = "owner_team_id", name = "owner_team"),
                @Index(columnList = "owner_user_id, owner_team_id", name = "owner_user__owner_team"),
                @Index(columnList = "status, owner_team_id", name = "status__owner_team"),
                @Index(columnList = "status, owner_user_id, owner_team_id", name = "status__owner_user__owner_team")
        }
)
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(
        discriminatorType = DiscriminatorType.INTEGER,
        name = "dtype",
        columnDefinition = "TINYINT"
)
public class IndexedFileEntity {
    public IndexedFileEntity(IndexedFileStatus status, Long ownerUserId, Long ownerTeamId) {
        this.status = status;
        this.ownerUserId = ownerUserId;
        this.ownerTeamId = ownerTeamId;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Convert(converter = IndexedFileStatusAttributeConverter.class)
    @Column(name = "status", nullable = false)
    private IndexedFileStatus status;

    @Column(name = "owner_user_id")
    private Long ownerUserId;

    @Column(name = "owner_team_id")
    private Long ownerTeamId;
}
