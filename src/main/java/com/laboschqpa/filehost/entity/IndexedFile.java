package com.laboschqpa.filehost.entity;

import javax.persistence.*;

@Entity
@Table(name = "indexed_file")
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(
        discriminatorType = DiscriminatorType.INTEGER,
        name = "type_discriminator",
        columnDefinition = "TINYINT"
)
public class IndexedFile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

}
