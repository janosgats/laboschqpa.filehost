package com.laboschqpa.filehost.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import com.laboschqpa.filehost.exceptions.NotImplementedException;

import java.util.Arrays;
import java.util.Optional;

public enum IndexedFileStatus {
    ADDED_TO_DATABASE_INDEX(0),
    UPLOADING(1),
    UPLOADED(2),
    AVAILABLE(3),
    DELETED(4),
    FAILED(5),
    ABORTED_BY_FILE_HOST(6),
    CLEANED_UP_AFTER_FAILED(7),
    CLEANED_UP_AFTER_ABORTED(8);

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
