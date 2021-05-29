package com.laboschqpa.filehost.enums;

import com.laboschqpa.filehost.exceptions.NotImplementedException;
import lombok.Getter;

import java.util.Arrays;
import java.util.Optional;

public enum S3Provider {
    UNDECIDED(0, "Not decided on provider yet", null),
    AMAZON(1, "Amazon", "amazonaws.com"),
    SCALE_WAY(2, "ScaleWay", "scw.cloud");

    @Getter
    private Integer value;
    @Getter
    private String displayName;
    @Getter
    private String endpointDomain;

    S3Provider(int value, String displayName, String endpointDomain) {
        this.value = value;
        this.displayName = displayName;
        this.endpointDomain = endpointDomain;
    }

    @Override
    public String toString() {
        return displayName;
    }

    public static S3Provider fromValue(Integer value) {
        Optional<S3Provider> optional = Arrays.stream(values())
                .filter(en -> en.getValue().equals(value))
                .findFirst();

        if (optional.isEmpty())
            throw new NotImplementedException("Enum from this value is not implemented");

        return optional.get();
    }

}
