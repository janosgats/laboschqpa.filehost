package com.laboschqpa.filehost.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import com.laboschqpa.filehost.exceptions.NotImplementedException;

import java.util.Arrays;
import java.util.Optional;

public enum IndexedFileStatus {
    ADDED_TO_DATABASE_INDEX(0),
    PROCESSING(1),
    AVAILABLE(2),
    FAILURE(3),
    DELETED(4);

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
