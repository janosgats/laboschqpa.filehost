package com.laboschqpa.filehost.service.imagevariant;

import com.laboschqpa.filehost.api.service.FileUploaderService;
import com.laboschqpa.filehost.entity.ImageVariant;
import com.laboschqpa.filehost.entity.IndexedFileEntity;
import com.laboschqpa.filehost.enums.ImageVariantStatus;
import com.laboschqpa.filehost.enums.IndexedFileStatus;
import com.laboschqpa.filehost.enums.UploadKind;
import com.laboschqpa.filehost.enums.UploadedFileType;
import com.laboschqpa.filehost.exceptions.ConflictingRequestDataApiException;
import com.laboschqpa.filehost.exceptions.apierrordescriptor.ContentNotFoundException;
import com.laboschqpa.filehost.model.upload.FileUploadRequest;
import com.laboschqpa.filehost.repo.ImageVariantRepository;
import com.laboschqpa.filehost.service.imagevariant.command.SaveImageVariantCommand;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;

@Log4j2
@RequiredArgsConstructor
@Service
public class VariantSaverService {
    private static final String METRIC_NAME_IMAGE_VARIANT_UPLOAD_COUNT = "image_variant_upload_count";
    private static final String TAG_NAME_RESULT = "result";
    private static final String TAG_VALUE_SUCCESS = "success";
    private static final String TAG_VALUE_FAILURE = "failure";

    private final MeterRegistry meterRegistry;
    private final TransactionTemplate transactionTemplate;
    private final ImageVariantRepository imageVariantRepository;

    private final FileUploaderService fileUploaderService;

    public IndexedFileEntity saveVariant(SaveImageVariantCommand command) {
        try {
            final IndexedFileEntity savedFileEntity = saveVariantInternal(command);

            meterRegistry.counter(METRIC_NAME_IMAGE_VARIANT_UPLOAD_COUNT,
                    TAG_NAME_RESULT, TAG_VALUE_SUCCESS).increment();
            return savedFileEntity;
        } catch (Throwable throwable) {
            meterRegistry.counter(METRIC_NAME_IMAGE_VARIANT_UPLOAD_COUNT,
                    TAG_NAME_RESULT, TAG_VALUE_FAILURE).increment();
            throw throwable;
        }
    }

    public IndexedFileEntity saveVariantInternal(SaveImageVariantCommand command) {
        final long jobId = command.getJobId();
        final ImageVariant variantUnderSave = getPreparedVariantForUpload(jobId);

        final FileUploadRequest fileUploadRequest = new FileUploadRequest(
                variantUnderSave.getOriginalFile().getOwnerUserId(),
                variantUnderSave.getOriginalFile().getOwnerTeamId(),
                UploadKind.IMAGE_VARIANT,
                UploadedFileType.IMAGE);

        try {
            final IndexedFileEntity savedFileEntity
                    = fileUploaderService.uploadFile(fileUploadRequest, command.getHttpServletRequest());

            variantUnderSave.setStatus(ImageVariantStatus.SUCCEEDED);
            variantUnderSave.setStatusUpdated(Instant.now());
            variantUnderSave.setVariantFile(savedFileEntity);
            imageVariantRepository.save(variantUnderSave);

            return savedFileEntity;
        } catch (Exception e) {
            transactionTemplate.executeWithoutResult(transactionStatus ->
                    imageVariantRepository.updateStatus(jobId, ImageVariantStatus.FAILED_DURING_UPLOAD, Instant.now())
            );
            throw e;
        }
    }

    private ImageVariant getPreparedVariantForUpload(long jobId) {
        return transactionTemplate.execute(transactionStatus -> {
            final ImageVariant imageVariant = imageVariantRepository.findById_withEagerOriginalFile_withPessimisticWriteLock(jobId)
                    .orElseThrow(() -> new ContentNotFoundException("Cannot find ImageVariant by jobId: " + jobId));

            if (ImageVariantStatus.SUCCEEDED == imageVariant.getStatus()) {
                throw new ConflictingRequestDataApiException("The ImageVariant already succeeded. jobId: " + jobId);
            }
            if (ImageVariantStatus.UNDER_UPLOAD == imageVariant.getStatus()) {
                throw new ConflictingRequestDataApiException("The ImageVariant is already under upload. jobId: " + jobId);
            }
            if (IndexedFileStatus.AVAILABLE != imageVariant.getOriginalFile().getStatus()) {
                throw new ConflictingRequestDataApiException("The original file of ImageVariant is not available. jobId: " + jobId);
            }

            imageVariant.setStatus(ImageVariantStatus.UNDER_UPLOAD);
            imageVariant.setStatusUpdated(Instant.now());

            return imageVariantRepository.save(imageVariant);
        });
    }
}
