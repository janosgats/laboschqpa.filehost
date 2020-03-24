package com.laboschcst.filehost.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import com.laboschcst.filehost.exceptions.NotImplementedException;

import java.util.Arrays;
import java.util.Optional;

public enum FileAccessType {
    READ(0),
    DELETE(1);

    private Integer value;

    FileAccessType(Integer value) {
        this.value = value;
    }

    @JsonValue
    public Integer getValue() {
        return value;
    }

    public static StoredFileStatus fromValue(Integer value) {
        Optional<StoredFileStatus> optional = Arrays.stream(StoredFileStatus.values())
                .filter(en -> en.getValue().equals(value))
                .findFirst();

        if (optional.isEmpty())
            throw new NotImplementedException("Enum from this value is not implemented");

        return optional.get();
    }
}
