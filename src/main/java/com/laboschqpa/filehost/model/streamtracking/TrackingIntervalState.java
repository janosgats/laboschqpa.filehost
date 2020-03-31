package com.laboschqpa.filehost.model.streamtracking;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TrackingIntervalState {
    long absoluteTrackedValue;
    long trackedValueDifference;
    long elapsedMillis;
}
