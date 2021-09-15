package com.laboschqpa.filehost.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import com.laboschqpa.filehost.exceptions.NotImplementedException;

import java.util.Arrays;
import java.util.Optional;

public enum ImageVariantStatus {
    WAITING_FOR_FIRST_PICKUP(1),
    PICKED_UP(2),
    CREATION_JOB_QUEUED(3),
    UNDER_UPLOAD(4),
    SUCCEEDED(5),
    FAILED_DURING_QUEUEING(6),
    FAILED_DURING_UPLOAD(7),
    FAILED_IN_JOB_PROCESSOR(8),
    /**
     * The image was transcoded and the variant exists but the image is corrupt.
     */
    EXISTS_CORRUPTED(9);

    private Integer value;

    ImageVariantStatus(Integer value) {
        this.value = value;
    }

    @JsonValue
    public Integer getValue() {
        return value;
    }

    public static ImageVariantStatus fromValue(int value) {
        Optional<ImageVariantStatus> optional = Arrays.stream(values())
                .filter(en -> en.getValue().equals(value))
                .findFirst();

        return optional.orElseThrow(() -> new NotImplementedException("Enum from this value is not implemented"));
    }
}
