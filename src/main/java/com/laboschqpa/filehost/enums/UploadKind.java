package com.laboschqpa.filehost.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import com.laboschqpa.filehost.exceptions.NotImplementedException;
import lombok.Getter;

import java.util.Arrays;
import java.util.Optional;

public enum UploadKind {
    BY_USER(1, true),
    IMAGE_VARIANT(2, false);

    private Integer value;
    @Getter
    private boolean shouldEnforceUserAndTeamQuota;

    UploadKind(Integer value, boolean shouldEnforceUserAndTeamQuota) {
        this.value = value;
        this.shouldEnforceUserAndTeamQuota = shouldEnforceUserAndTeamQuota;
    }

    @JsonValue
    public Integer getValue() {
        return value;
    }

    public static UploadKind fromValue(Integer value) {
        Optional<UploadKind> optional = Arrays.stream(UploadKind.values())
                .filter(en -> en.getValue().equals(value))
                .findFirst();

        if (optional.isEmpty())
            throw new NotImplementedException("Enum from this value is not implemented");

        return optional.get();
    }
}
