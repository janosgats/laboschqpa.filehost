package com.laboschqpa.filehost.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import com.laboschqpa.filehost.exceptions.NotImplementedException;

import java.util.Arrays;
import java.util.Optional;

public enum IndexedFileStatus {
    ADDED_TO_DATABASE_INDEX(0),
    PRE_UPLOAD_PROCESSING(1),
    UPLOADING(2),
    UPLOAD_STREAM_SAVED(3),
    AVAILABLE(4),
    DELETED(5),
    FAILED(6),
    ABORTED_BY_FILE_HOST(7),
    CLEANED_UP_AFTER_FAILED(8),
    CLEANED_UP_AFTER_ABORTED(9),
    FAILED_DURING_DELETION(10);

    private Integer value;

    IndexedFileStatus(Integer value) {
        this.value = value;
    }

    @JsonValue
    public Integer getValue() {
        return value;
    }

    public static IndexedFileStatus fromValue(Integer value) {
        Optional<IndexedFileStatus> optional = Arrays.stream(IndexedFileStatus.values())
                .filter(en -> en.getValue().equals(value))
                .findFirst();

        if (optional.isEmpty())
            throw new NotImplementedException("Enum from this value is not implemented");

        return optional.get();
    }
}
