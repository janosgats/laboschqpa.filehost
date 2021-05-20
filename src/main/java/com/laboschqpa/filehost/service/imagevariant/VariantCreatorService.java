package com.laboschqpa.filehost.service.imagevariant;

import com.laboschqpa.filehost.entity.ImageVariant;
import com.laboschqpa.filehost.entity.IndexedFileEntity;
import com.laboschqpa.filehost.enums.ImageVariantStatus;
import com.laboschqpa.filehost.repo.ImageVariantJdbcRepository;
import com.laboschqpa.filehost.repo.ImageVariantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Log4j2
@RequiredArgsConstructor
@Service
public class VariantCreatorService {
    private static final int MISSING_CREATION_BATCH_LIMIT = 1000;
    private static final int MAX_ROUNDS_FOR_ONE_SIZE = 5;
    private static final int[] VARIANT_SIZES = new int[]{250, 500, 1000, 2000};

    private final ImageVariantJdbcRepository imageVariantJdbcRepository;
    private final ImageVariantRepository imageVariantRepository;

    public void createSomeMissingVariants() {
        Exception lastCaughtException = null;

        for (int currentSize : VARIANT_SIZES) {
            try {
                createSomeMissingVariantsForSize(currentSize);
            } catch (Exception e) {
                log.error("Exception while Creating Missing Variants for variantSize: {}", currentSize, e);
                lastCaughtException = e;
            }
        }

        if (lastCaughtException != null) {
            throw new RuntimeException("Exception while Creating Missing Variants - " +
                    lastCaughtException.getClass().getName() + ": " + lastCaughtException.getMessage(),
                    lastCaughtException);
        }
    }

    public void createSomeMissingVariantsForSize(int variantSize) throws Exception {
        Exception lastCaughtException = null;

        for (int i = 0; i < MAX_ROUNDS_FOR_ONE_SIZE; ++i) {
            try {
                final int foundIdsCount = createOneBatchOfMissingVariantsForSize(variantSize);
                if (foundIdsCount == 0) {
                    break;
                }
            } catch (Exception e) {
                log.error("Exception while Creating One Batch of Missing Variants for variantSize: {}", variantSize, e);
                lastCaughtException = e;
            }
        }

        if (lastCaughtException != null) {
            throw lastCaughtException;
        }
    }

    private int createOneBatchOfMissingVariantsForSize(int variantSize) {
        final List<Long> idsToCreateVariantsFor
                = imageVariantJdbcRepository.listImageIdsWithoutVariantOfSizeToCreateVariantsFor(
                variantSize, MISSING_CREATION_BATCH_LIMIT);

        final int foundIdsCount = idsToCreateVariantsFor.size();
        if (foundIdsCount == 0) {
            return foundIdsCount;
        }
        log.trace("Found {} pcs of missing Image Variants for size {}", idsToCreateVariantsFor.size(), variantSize);

        final Instant now = Instant.now();

        final List<ImageVariant> variantsToSave = new ArrayList<>(idsToCreateVariantsFor.size());
        for (long originalFileId : idsToCreateVariantsFor) {
            variantsToSave.add(createNewMissingVariantEntity(originalFileId, variantSize, now));
        }
        imageVariantRepository.saveAll(variantsToSave);
        imageVariantRepository.flush();
        log.debug("Created {} pcs of Image Variants for size {}", variantsToSave.size(), variantSize);

        return foundIdsCount;
    }

    public void createMissingVariantsForFile(IndexedFileEntity indexedFileEntity) {
        if (!indexedFileEntity.getIsImage()) {
            return;
        }

        final long originalFileId = indexedFileEntity.getId();
        final List<ImageVariant> existingVariants = imageVariantRepository.findAllByOriginalFileId(originalFileId);
        final Set<Integer> existingVariantSizes = existingVariants.stream()
                .map(ImageVariant::getVariantSize)
                .collect(Collectors.toSet());

        final Instant now = Instant.now();
        final List<ImageVariant> variantsToSave = new ArrayList<>(VARIANT_SIZES.length);
        for (int currentSize : VARIANT_SIZES) {
            if (!existingVariantSizes.contains(currentSize)) {
                variantsToSave.add(createNewMissingVariantEntity(originalFileId, currentSize, now));
            }
        }
        imageVariantRepository.saveAll(variantsToSave);
        imageVariantRepository.flush();
        log.debug("Created {} pcs of Image Variants for file: {}",
                variantsToSave.size(), originalFileId);
    }

    private ImageVariant createNewMissingVariantEntity(long originalFileId, int variantSize, Instant now) {
        ImageVariant imageVariant = new ImageVariant();
        imageVariant.setOriginalFile(new IndexedFileEntity(originalFileId));
        imageVariant.setVariantSize(variantSize);
        imageVariant.setStatus(ImageVariantStatus.WAITING_FOR_FIRST_PICKUP);
        imageVariant.setStatusUpdated(now);
        imageVariant.setTrialsCount(0);

        return imageVariant;
    }
}
