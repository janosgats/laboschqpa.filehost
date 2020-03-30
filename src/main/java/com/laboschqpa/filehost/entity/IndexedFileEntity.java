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
@Table(name = "indexed_file")
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(
        discriminatorType = DiscriminatorType.INTEGER,
        name = "type_discriminator",
        columnDefinition = "TINYINT"
)
public class IndexedFileEntity {
    public IndexedFileEntity(IndexedFileStatus status) {
        this.status = status;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Convert(converter = IndexedFileStatusAttributeConverter.class)
    @Column(name = "status", nullable = false)
    private IndexedFileStatus status;
}
