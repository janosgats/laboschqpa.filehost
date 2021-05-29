package com.laboschqpa.filehost.entity;

import com.laboschqpa.filehost.enums.FileServingEventKind;
import com.laboschqpa.filehost.enums.attributeconverter.FileServingEventKindAttributeConverter;
import lombok.Data;

import javax.persistence.*;
import java.time.Instant;

@Data
@Entity
@Table(name = "file_serving_event",
        indexes = {
                @Index(columnList = "event_kind, requester_user_id, time", name = "event_kind__requester_user_id__time"),
                @Index(columnList = "time, event_kind", name = "time__event_kind")
        }
)
public class FileServingEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Convert(converter = FileServingEventKindAttributeConverter.class)
    @Column(name = "event_kind", nullable = false)
    private FileServingEventKind eventKind;

    @Column(name = "requester_user_id", nullable = false)
    private Long requesterUserId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requested_file_id", nullable = false)
    private IndexedFileEntity requestedFile;

    @Column(name = "time", columnDefinition = "datetime", nullable = false)
    private Instant time;
}
