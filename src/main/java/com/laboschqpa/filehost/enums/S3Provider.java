package com.laboschqpa.filehost.enums;

import com.laboschqpa.filehost.exceptions.NotImplementedException;
import lombok.Getter;

import java.util.Arrays;
import java.util.Optional;

public enum S3Provider {
    AMAZON(0, "Amazon"),
    SCALE_WAY(1, "ScaleWay");

    @Getter
    private Integer value;
    @Getter
    private String displayName;

    S3Provider(int value, String displayName) {
        this.value = value;
        this.displayName = displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }

    public static S3Provider fromValue(Integer value) {
        Optional<S3Provider> optional = Arrays.stream(S3Provider.values())
                .filter(en -> en.getValue().equals(value))
                .findFirst();

        if (optional.isEmpty())
            throw new NotImplementedException("Enum from this value is not implemented");

        return optional.get();
    }

}
