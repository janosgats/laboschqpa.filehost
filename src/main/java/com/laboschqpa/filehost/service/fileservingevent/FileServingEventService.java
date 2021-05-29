package com.laboschqpa.filehost.service.fileservingevent;

import com.laboschqpa.filehost.entity.FileServingEvent;
import com.laboschqpa.filehost.entity.IndexedFileEntity;
import com.laboschqpa.filehost.enums.FileServingEventKind;
import com.laboschqpa.filehost.repo.FileServingEventRepository;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Log4j2
@RequiredArgsConstructor
@Service
public class FileServingEventService {
    private static final String METRIC_NAME_FILE_SERVING_EVENT_COUNT = "file_serving_event_count";
    private static final String TAG_NAME_EVENT_KIND = "eventKind";

    private static final String METRIC_NAME_FILE_SERVING_RATE_LIMIT_HIT_COUNT = "file_serving_rate_limit_hit_count";
    private static final String TAG_NAME_RATE_LIMIT_TYPE = "rateLimitType";
    private static final String TAG_VALUE_TEN_MINUTELY = "tenMinutely";
    private static final String TAG_VALUE_HOURLY = "hourly";
    private static final String TAG_VALUE_DAILY = "daily";

    private static final long SECONDS_IN_MINUTE = 60;
    private static final long SECONDS_IN_TEN_MINUTES = 10 * SECONDS_IN_MINUTE;
    private static final long SECONDS_IN_HOUR = 60 * SECONDS_IN_MINUTE;
    private static final long SECONDS_IN_DAY = 24 * SECONDS_IN_HOUR;

    private final MeterRegistry meterRegistry;
    private final FileServingEventRepository fileServingEventRepository;

    @Value("${fileServing.rateLimit.tenMinutely.count}")
    private Long countLimitTenMinutely;
    @Value("${fileServing.rateLimit.hourly.count}")
    private Long countLimitHourly;
    @Value("${fileServing.rateLimit.daily.count}")
    private Long countLimitDaily;

    public void log(FileServingEventKind eventKind, long requesterUserId, long requestedFileId) {
        FileServingEvent fileServingEvent = new FileServingEvent();

        fileServingEvent.setEventKind(eventKind);
        fileServingEvent.setRequesterUserId(requesterUserId);
        fileServingEvent.setRequestedFile(new IndexedFileEntity(requestedFileId));
        fileServingEvent.setTime(Instant.now());

        fileServingEventRepository.save(fileServingEvent);
        log.debug("Saved FileServingEvent: id: {}, kind: {}, requesterUser: {}, requestedFile: {}",
                fileServingEvent.getId(), eventKind, requesterUserId, requestedFileId);
        meterRegistry.counter(METRIC_NAME_FILE_SERVING_EVENT_COUNT,
                TAG_NAME_EVENT_KIND, eventKind.getValue().toString()).increment();
    }

    public boolean isRateLimitAlright(long requesterUserId) {
        final Instant now = Instant.now();
        final Instant sinceTimeForTenMinutely = now.minusSeconds(SECONDS_IN_TEN_MINUTES);
        final Instant sinceTimeForHourly = now.minusSeconds(SECONDS_IN_HOUR);
        final Instant sinceTimeForDaily = now.minusSeconds(SECONDS_IN_DAY);


        final long tenMinutelyCount = fileServingEventRepository.countOfEventsSince(
                FileServingEventKind.SERVING_RESPONSE_CREATED_SUCCESSFULLY,
                requesterUserId, sinceTimeForTenMinutely);
        final long hourlyCount = fileServingEventRepository.countOfEventsSince(
                FileServingEventKind.SERVING_RESPONSE_CREATED_SUCCESSFULLY,
                requesterUserId, sinceTimeForHourly);
        final long dailyCount = fileServingEventRepository.countOfEventsSince(
                FileServingEventKind.SERVING_RESPONSE_CREATED_SUCCESSFULLY,
                requesterUserId, sinceTimeForDaily);

        final boolean isOkayTenMinutely = tenMinutelyCount < countLimitTenMinutely;
        final boolean isOkayHourly = hourlyCount < countLimitHourly;
        final boolean isOkayDaily = dailyCount < countLimitDaily;

        if (!isOkayTenMinutely) {
            meterRegistry.counter(METRIC_NAME_FILE_SERVING_RATE_LIMIT_HIT_COUNT,
                    TAG_NAME_RATE_LIMIT_TYPE, TAG_VALUE_TEN_MINUTELY).increment();
        }
        if (!isOkayHourly) {
            meterRegistry.counter(METRIC_NAME_FILE_SERVING_RATE_LIMIT_HIT_COUNT,
                    TAG_NAME_RATE_LIMIT_TYPE, TAG_VALUE_HOURLY).increment();
        }
        if (!isOkayDaily) {
            meterRegistry.counter(METRIC_NAME_FILE_SERVING_RATE_LIMIT_HIT_COUNT,
                    TAG_NAME_RATE_LIMIT_TYPE, TAG_VALUE_DAILY).increment();
        }

        return isOkayTenMinutely && isOkayHourly && isOkayDaily;
    }
}
