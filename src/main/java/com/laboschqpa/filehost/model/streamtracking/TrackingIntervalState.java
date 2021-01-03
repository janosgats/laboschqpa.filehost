package com.laboschqpa.filehost.model.streamtracking;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class TrackingIntervalState {
    long absoluteTrackedValue;
    long trackedValueDifference;
    long elapsedMillis;
}
