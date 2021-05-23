package com.laboschqpa.filehost.service.imagevariant;

import com.laboschqpa.filehost.entity.ImageVariant;
import com.laboschqpa.filehost.enums.ImageVariantStatus;
import com.laboschqpa.filehost.exceptions.ConflictingRequestDataApiException;
import com.laboschqpa.filehost.exceptions.apierrordescriptor.ContentNotFoundException;
import com.laboschqpa.filehost.repo.ImageVariantRepository;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;

@Log4j2
@RequiredArgsConstructor
@Service
public class VariantService {
    private static final String SIGNALED_IMAGE_VARIANT_JOB_FAILED_IN_JOB_PROCESSOR_COUNT = "signaled_image_variant_job_failed_in_job_processor_count";

    private final MeterRegistry meterRegistry;
    private final TransactionTemplate transactionTemplate;
    private final ImageVariantRepository imageVariantRepository;


    public void signalJobFailedInJobProcessor(long jobId) {
        log.info("Received signal of ImageVariant job failed in job processor. jobId: {}", jobId);
        meterRegistry.counter(SIGNALED_IMAGE_VARIANT_JOB_FAILED_IN_JOB_PROCESSOR_COUNT).increment();

        transactionTemplate.executeWithoutResult(transactionStatus -> {
            /* TODO: Change this to optimistic locking. The intention is: not to mark jobs as failed
                if another state change happens to them before writing this to the DB.
                Pessimistic lock is working, but optimistic would be more appropriate.
             */
            final ImageVariant imageVariant = imageVariantRepository.findById_withPessimisticWriteLock(jobId)
                    .orElseThrow(() -> new ContentNotFoundException("Cannot find ImageVariant by jobId: " + jobId));

            if (ImageVariantStatus.SUCCEEDED == imageVariant.getStatus()) {
                throw new ConflictingRequestDataApiException("The ImageVariant already succeeded. jobId: " + jobId);
            }
            if (ImageVariantStatus.UNDER_UPLOAD == imageVariant.getStatus()) {
                throw new ConflictingRequestDataApiException("The ImageVariant is already under upload. jobId: " + jobId);
            }

            imageVariant.setStatus(ImageVariantStatus.FAILED_IN_JOB_PROCESSOR);
            imageVariant.setStatusUpdated(Instant.now());

            imageVariantRepository.save(imageVariant);
        });
    }

}
