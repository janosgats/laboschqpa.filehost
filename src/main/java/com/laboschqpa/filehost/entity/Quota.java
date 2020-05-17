package com.laboschqpa.filehost.entity;

import com.laboschqpa.filehost.enums.QuotaSubjectCategory;
import com.laboschqpa.filehost.enums.attributeconverter.QuotaSubjectCategoryAttributeConverter;
import lombok.Data;

import javax.persistence.*;

@Data
@Entity
@Table(name = "quota",
        indexes = {
                @Index(columnList = "subject_category", name = "subject_category"),
                @Index(columnList = "limit_bytes", name = "limit_bytes"),
                @Index(columnList = "subject_category, limit_bytes", name = "subject_category__limit_bytes"),
                @Index(columnList = "subject_id, limit_bytes", name = "subject_id__limit_bytes")
        },
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"subject_id", "subject_category"}, name = "subject_id__subject_category__unique")
        }
)
public class Quota {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Convert(converter = QuotaSubjectCategoryAttributeConverter.class)
    @Column(name = "subject_category", nullable = false)
    private QuotaSubjectCategory subjectCategory;

    @Column(name = "subject_id", nullable = false)
    private Long subjectId;

    @Column(name = "limit_bytes", nullable = false)
    private Long limitBytes;
}
