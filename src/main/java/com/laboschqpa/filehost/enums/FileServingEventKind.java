package com.laboschqpa.filehost.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import com.laboschqpa.filehost.exceptions.NotImplementedException;

import java.util.Arrays;
import java.util.Optional;

public enum FileServingEventKind {
    SERVING_RESPONSE_CREATED_SUCCESSFULLY(1),
    ERROR_WHILE_CREATING_SERVING_RESPONSE(2),
    DENIED_BECAUSE_OF_RATE_LIMIT(3);

    private Integer value;

    FileServingEventKind(Integer value) {
        this.value = value;
    }

    @JsonValue
    public Integer getValue() {
        return value;
    }

    public static FileServingEventKind fromValue(Integer value) {
        Optional<FileServingEventKind> optional = Arrays.stream(values())
                .filter(en -> en.getValue().equals(value))
                .findFirst();

        if (optional.isEmpty())
            throw new NotImplementedException("Enum from this value is not implemented");

        return optional.get();
    }
}
