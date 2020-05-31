package com.laboschqpa.filehost.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import com.laboschqpa.filehost.exceptions.NotImplementedException;

import java.util.Arrays;
import java.util.Optional;

public enum FileAccessType {
    READ(0),
    DELETE(1),
    CREATE_NEW(2),
    EDIT(3);

    private Integer value;

    FileAccessType(Integer value) {
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
