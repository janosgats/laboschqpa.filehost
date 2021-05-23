package com.laboschqpa.filehost.service.imagevariant;

import com.laboschqpa.filehost.entity.ImageVariant;
import com.laboschqpa.filehost.enums.ImageVariantStatus;
import com.laboschqpa.filehost.enums.IndexedFileStatus;
import com.laboschqpa.filehost.exceptions.apierrordescriptor.ContentNotFoundException;
import com.laboschqpa.filehost.repo.ImageVariantRepository;
import com.laboschqpa.filehost.service.apiclient.imageconverter.ImageConverterApiClient;
import com.laboschqpa.filehost.service.apiclient.imageconverter.dto.ProcessCreationJobRequestDto;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.List;

@Log4j2
@RequiredArgsConstructor
@Service
public class VariantJobPickupService {
    private static final String PICKED_UP_IMAGE_VARIANT_JOB_COUNT = "picked_up_image_variant_job_count";
    private static final String TAG_NAME_QUEUING_RESULT = "queuingResult";
    private static final String TAG_VALUE_SUCCESS = "success";
    private static final String TAG_VALUE_FAILURE = "failure";

    private static final int PICK_UP_FAILED_JOBS_TIMEOUT_SECONDS = 10 * 60;
    private static final int PICK_UP_UNFINISHED_JOBS_TIMEOUT_SECONDS = 30 * 60;

    private static final int MAX_TRIALS_ON_A_JOB = 25;
    private static final List<ImageVariantStatus> STATUSES_FOR_INSTANT_PICKUP = List.of(ImageVariantStatus.WAITING_FOR_FIRST_PICKUP);
    private static final List<ImageVariantStatus> FAILED_STATUSES = List.of(
            ImageVariantStatus.FAILED_DURING_QUEUEING,
            ImageVariantStatus.FAILED_DURING_UPLOAD,
            ImageVariantStatus.FAILED_IN_JOB_PROCESSOR
    );
    private static final List<ImageVariantStatus> UNFINISHED_STATUSES = List.of(
            ImageVariantStatus.PICKED_UP,
            ImageVariantStatus.CREATION_JOB_QUEUED,
            ImageVariantStatus.UNDER_UPLOAD
    );


    private final MeterRegistry meterRegistry;
    private final TransactionTemplate transactionTemplate;
    private final ImageConverterApiClient imageConverterApiClient;
    private final ImageVariantRepository imageVariantRepository;

    @Value("${imageVariantJobs.limitOfJobsToPickUp}")
    private Integer limitOfJobsToPickUp;

    public void pickUpSomeCreationJobs() {
        List<Long> idsToPickUp = imageVariantRepository.getJobIdsToPickUp(
                STATUSES_FOR_INSTANT_PICKUP,
                FAILED_STATUSES,
                Instant.now().minusSeconds(PICK_UP_FAILED_JOBS_TIMEOUT_SECONDS),
                UNFINISHED_STATUSES,
                Instant.now().minusSeconds(PICK_UP_UNFINISHED_JOBS_TIMEOUT_SECONDS),
                MAX_TRIALS_ON_A_JOB,
                PageRequest.of(0, limitOfJobsToPickUp)
        );

        if (!idsToPickUp.isEmpty()) {
            log.debug("Picked up {} Image Variant jobs. jobIds: {}", idsToPickUp.size(), idsToPickUp);
        }

        for (long jobId : idsToPickUp) {
            tryToQueueJob(jobId);//TODO: Use an executor on a threadpool and parallelize this
        }
    }

    private void tryToQueueJob(long jobId) {
        boolean isSucceeded = false;
        try {
            queueJob(jobId);
            isSucceeded = true;
        } catch (Exception e) {
            log.error("Exception caught while queuing job. jobId: {}", jobId, e);
            try {
                transactionTemplate.executeWithoutResult(transactionStatus ->
                        imageVariantRepository.updateStatus(jobId, ImageVariantStatus.FAILED_DURING_QUEUEING, Instant.now())
                );
            } catch (Exception innerException) {
                log.error("Exception caught while setting ImageVariant status to failed " +
                        "after error during queueing the job. jobId: {}", jobId, innerException);
            }
        } finally {
            final String resultTag = isSucceeded ? TAG_VALUE_SUCCESS : TAG_VALUE_FAILURE;
            try {
                meterRegistry.counter(PICKED_UP_IMAGE_VARIANT_JOB_COUNT,
                        TAG_NAME_QUEUING_RESULT, resultTag
                ).increment();
            } catch (Exception innerException) {
                log.error("Exception caught while incrementing MeterRegistry counter after ImageVariant job pickup. " +
                        "resultTag: {}, jobId: {}", resultTag, jobId, innerException);
            }
        }
    }

    private void queueJob(long jobId) {
        transactionTemplate.executeWithoutResult(transactionStatus -> {
            final ImageVariant imageVariant = imageVariantRepository.findById_withEagerOriginalFile_withPessimisticWriteLock(jobId)
                    .orElseThrow(() -> new ContentNotFoundException("Cannot find ImageVariant by jobId: " + jobId));

            if (ImageVariantStatus.SUCCEEDED == imageVariant.getStatus()) {
                return;//The job somehow finished in the mean time
            }

            if (IndexedFileStatus.AVAILABLE != imageVariant.getOriginalFile().getStatus()) {
                //The ImageVariant is stale. We can delete it, since the original image no longer exists.
                imageVariantRepository.delete(imageVariant);
                return;
            }

            imageVariant.setStatus(ImageVariantStatus.PICKED_UP);
            imageVariant.setStatusUpdated(Instant.now());
            imageVariant.setTrialsCount(imageVariant.getTrialsCount() + 1);

            imageVariantRepository.save(imageVariant);

            final ProcessCreationJobRequestDto requestDto = new ProcessCreationJobRequestDto();
            requestDto.setJobId(imageVariant.getJobId());
            requestDto.setOriginalFileId(imageVariant.getOriginalFile().getId());
            requestDto.setVariantSize(imageVariant.getVariantSize());
            imageConverterApiClient.processImageVariantCreationJob(requestDto).block();


            imageVariant.setStatus(ImageVariantStatus.CREATION_JOB_QUEUED);
            imageVariant.setStatusUpdated(Instant.now());
            imageVariantRepository.save(imageVariant);
        });
    }

}
