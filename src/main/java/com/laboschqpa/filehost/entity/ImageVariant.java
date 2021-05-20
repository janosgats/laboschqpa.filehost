package com.laboschqpa.filehost.entity;

import com.laboschqpa.filehost.enums.ImageVariantStatus;
import com.laboschqpa.filehost.enums.attributeconverter.ImageVariantStatusAttributeConverter;
import lombok.Data;

import javax.persistence.*;
import java.time.Instant;

@Data
@Entity
@Table(name = "image_variant",
        indexes = {
                @Index(columnList = "trials_count, status", name = "trials_count__status"),
                @Index(columnList = "status_updated, trials_count", name = "status_updated__trials_count"),
                @Index(columnList = "status, status_updated, trials_count, variant_size", name = "status__status_updated__trials_count__variant_size")
        },
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"original_file_id", "variant_size"}, name = "original_file_id__variant_size__unique"),
                @UniqueConstraint(columnNames = {"variant_file_id"}, name = "variant_file_id__unique")
        }
)
public class ImageVariant {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "job_id")
    private Long jobId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "original_file_id", nullable = false)
    private IndexedFileEntity originalFile;

    @Column(name = "variant_size", nullable = false)
    private Integer variantSize;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_file_id")
    private IndexedFileEntity variantFile;

    @Convert(converter = ImageVariantStatusAttributeConverter.class)
    @Column(name = "status", nullable = false)
    private ImageVariantStatus status;

    @Column(name = "status_updated", columnDefinition = "datetime", nullable = false)
    private Instant statusUpdated;

    @Column(name = "trials_count", nullable = false)
    private Integer trialsCount;
}
