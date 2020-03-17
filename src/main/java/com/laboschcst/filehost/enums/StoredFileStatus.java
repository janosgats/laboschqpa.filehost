package com.laboschcst.filehost.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import com.laboschcst.filehost.exceptions.NotImplementedException;

import java.util.Arrays;
import java.util.Optional;

public enum StoredFileStatus {
    SAVED_TO_DATABASE(0),
    UPLOADING(1),
    PROCESSING(2),
    AVAILABLE(3);

    private Integer value;

    StoredFileStatus(Integer value) {
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
