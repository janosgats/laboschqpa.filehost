package com.laboschqpa.filehost.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import com.laboschqpa.filehost.exceptions.NotImplementedException;

import java.util.Arrays;
import java.util.Optional;

public enum QuotaSubjectCategory {
    USER(1),
    TEAM(2);

    private Integer value;

    QuotaSubjectCategory(Integer value) {
        this.value = value;
    }

    @JsonValue
    public Integer getValue() {
        return value;
    }

    public static QuotaSubjectCategory fromValue(Integer value) {
        Optional<QuotaSubjectCategory> optional = Arrays.stream(values())
                .filter(en -> en.getValue().equals(value))
                .findFirst();

        if (optional.isEmpty())
            throw new NotImplementedException("Enum from this value is not implemented");

        return optional.get();
    }
}
