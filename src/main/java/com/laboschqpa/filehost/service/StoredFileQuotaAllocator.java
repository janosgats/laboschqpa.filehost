package com.laboschqpa.filehost.service;

import com.laboschqpa.filehost.entity.Quota;
import com.laboschqpa.filehost.entity.StoredFileEntity;
import com.laboschqpa.filehost.enums.QuotaSubjectCategory;
import com.laboschqpa.filehost.exceptions.fileserving.QuotaAllocationException;
import com.laboschqpa.filehost.model.quota.QuotaResourceUsageJpaDto;
import com.laboschqpa.filehost.model.streamtracking.TrackingInputStream;
import com.laboschqpa.filehost.repo.QuotaRepository;
import com.laboschqpa.filehost.repo.StoredFileEntityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import com.laboschqpa.filehost.exceptions.fileserving.QuotaExceededException;

import java.util.List;
import java.util.Optional;

@Log4j2
@RequiredArgsConstructor
@Service
public class StoredFileQuotaAllocator {
    private final QuotaRepository quotaRepository;
    private final StoredFileEntityRepository storedFileEntityRepository;

    @Value("${quota.default.user}")
    private Long defaultUserQuotaLimitBytes;
    @Value("${quota.default.team}")
    private Long defaultTeamQuotaLimitBytes;

    /**
     * @return The amount of allocated bytes.
     * @throws QuotaExceededException if no quota left
     */
    public long allocateQuota(StoredFileEntity storedFileEntity, TrackingInputStream trackingInputStream, Long approximateFileSize) {
        final Quota userQuota = getQuota(QuotaSubjectCategory.USER, storedFileEntity.getOwnerUserId());
        final Quota teamQuota = getQuota(QuotaSubjectCategory.TEAM, storedFileEntity.getOwnerTeamId());

        final Pair<QuotaResourceUsageJpaDto, QuotaResourceUsageJpaDto> resourceUsages = getQuotaResourceUsage(storedFileEntity);
        final QuotaResourceUsageJpaDto userResourceUsageBefore = resourceUsages.getFirst();
        final QuotaResourceUsageJpaDto teamResourceUsageBefore = resourceUsages.getSecond();

        assertQuotaIsNotExceeded(userQuota, userResourceUsageBefore, teamQuota, teamResourceUsageBefore);

        final long userFreeBytes = userQuota.getLimitBytes() - userResourceUsageBefore.getUsedBytes();
        final long teamFreeBytes = teamQuota.getLimitBytes() - teamResourceUsageBefore.getUsedBytes();

        if (approximateFileSize != null && trackingInputStream.getCountOfReadBytes() < (approximateFileSize * 0.9d)) {
            long newBytesToAllocate = approximateFileSize - trackingInputStream.getCountOfReadBytes();

            if (userFreeBytes < newBytesToAllocate) {
                throw new QuotaExceededException(QuotaSubjectCategory.USER,
                        "Uploading the file would exceed user storage quota!");
            }
            if (teamFreeBytes < newBytesToAllocate) {
                throw new QuotaExceededException(QuotaSubjectCategory.TEAM,
                        "Uploading the file would exceed team storage quota!");
            }

            long completeFileSize = trackingInputStream.getCountOfReadBytes() + newBytesToAllocate;
            allocateBytes(completeFileSize, storedFileEntity, userQuota, teamQuota);
            return newBytesToAllocate;
        } else if (approximateFileSize != null) {
            final long wantedNewBytes;
            if (approximateFileSize / (double) trackingInputStream.getCountOfReadBytes() < 2) {
                wantedNewBytes = (long) (approximateFileSize * 0.34d);
            } else {
                wantedNewBytes = (long) (trackingInputStream.getCountOfReadBytes() * 0.34d);
            }

            final long newBytesToAllocate = limitBytesAccordingToFreeSpace(wantedNewBytes, userFreeBytes, teamFreeBytes);
            final long completeFileSize = trackingInputStream.getCountOfReadBytes() + newBytesToAllocate;
            allocateBytes(completeFileSize, storedFileEntity, userQuota, teamQuota);
            return newBytesToAllocate;
        } else {
            final long wantedNewBytes;
            if (trackingInputStream.getCountOfReadBytes() < 13 * 1000 * 1000) {
                wantedNewBytes = 5 * 1000 * 1000;
            } else {
                wantedNewBytes = (long) (trackingInputStream.getCountOfReadBytes() * 0.34d);
            }

            final long newBytesToAllocate = limitBytesAccordingToFreeSpace(wantedNewBytes, userFreeBytes, teamFreeBytes);
            final long completeFileSize = trackingInputStream.getCountOfReadBytes() + newBytesToAllocate;
            allocateBytes(completeFileSize, storedFileEntity, userQuota, teamQuota);
            return newBytesToAllocate;
        }
        //TODO: Eliminate the magic numbers!
    }

    private long limitBytesAccordingToFreeSpace(long wantedNewBytes, long userFreeBytes, long teamFreeBytes) {
        final long userLimitedBytes = Math.min(wantedNewBytes, (long) (userFreeBytes * 0.99d));
        final long teamLimitedBytes = Math.min(wantedNewBytes, (long) (teamFreeBytes * 0.99d));

        if (userLimitedBytes < 1000) {
            throw new QuotaExceededException(QuotaSubjectCategory.USER,
                    "User storage quota is exceeded! Cannot allocate more space.");
        }
        if (teamLimitedBytes < 1000) {
            throw new QuotaExceededException(QuotaSubjectCategory.TEAM,
                    "Team storage quota is exceeded! Cannot allocate more space.");
        }

        return Math.min(userLimitedBytes, teamLimitedBytes);
    }

    private void allocateBytes(long completeFileSize, StoredFileEntity storedFileEntity, Quota userQuota, Quota teamQuota) {
        storedFileEntity.setSize(completeFileSize);
        storedFileEntityRepository.save(storedFileEntity);

        final Pair<QuotaResourceUsageJpaDto, QuotaResourceUsageJpaDto> resourceUsages = getQuotaResourceUsage(storedFileEntity);
        final QuotaResourceUsageJpaDto userResourceUsageBefore = resourceUsages.getFirst();
        final QuotaResourceUsageJpaDto teamResourceUsageBefore = resourceUsages.getSecond();

        assertQuotaIsNotExceeded(userQuota, userResourceUsageBefore, teamQuota, teamResourceUsageBefore);
        log.trace("Allocated storage for file: {}, user: {}, team: {}. completeFileSize: {}MB",
                storedFileEntity.getId(), userQuota.getSubjectId(), teamQuota.getSubjectId(), completeFileSize / 1000000d);
    }

    private void assertQuotaIsNotExceeded(Quota userQuota, QuotaResourceUsageJpaDto userResourceUsage, Quota teamQuota, QuotaResourceUsageJpaDto teamResourceUsage) {
        if (userResourceUsage.getUsedBytes() > userQuota.getLimitBytes()) {
            throw new QuotaExceededException(QuotaSubjectCategory.USER,
                    "User storage quota is exceeded at " + getUsagePercentage(userQuota, userResourceUsage) + " percents!");
        }
        if (teamResourceUsage.getUsedBytes() > teamQuota.getLimitBytes()) {
            throw new QuotaExceededException(QuotaSubjectCategory.TEAM,
                    "Team storage quota is exceeded at " + getUsagePercentage(teamQuota, teamResourceUsage) + " percents!");
        }
    }

    private Integer getUsagePercentage(Quota quota, QuotaResourceUsageJpaDto usage) {
        return Math.round((float) usage.getUsedBytes() / (float) quota.getLimitBytes() * 100);
    }

    private Quota getQuota(QuotaSubjectCategory subjectCategory, Long subjectId) {
        Optional<Quota> quotaOptional = quotaRepository.findBySubjectCategoryAndSubjectId(subjectCategory, subjectId);
        if (quotaOptional.isPresent()) {
            return quotaOptional.get();
        } else {
            Quota quota = new Quota();
            quota.setSubjectCategory(subjectCategory);
            quota.setSubjectId(subjectId);

            switch (subjectCategory) {
                case USER:
                    quota.setLimitBytes(defaultUserQuotaLimitBytes);
                    break;
                case TEAM:
                    quota.setLimitBytes(defaultTeamQuotaLimitBytes);
                    break;
                default:
                    throw new IllegalStateException("No default quota specified for QuotaSubjectCategory: " + subjectCategory);
            }

            quotaRepository.save(quota);
            return quota;
        }
    }

    private Pair<QuotaResourceUsageJpaDto, QuotaResourceUsageJpaDto> getQuotaResourceUsage(StoredFileEntity storedFileEntity) {
        List<QuotaResourceUsageJpaDto> resourceUsage
                = quotaRepository.getResourceQuotaUsage(storedFileEntity.getOwnerUserId(), storedFileEntity.getOwnerTeamId());

        QuotaResourceUsageJpaDto userResourceUsage = null, teamResourceUsage = null;
        for (QuotaResourceUsageJpaDto item : resourceUsage) {
            if (item.getSubjectCategory() == QuotaSubjectCategory.USER) {
                userResourceUsage = item;
            } else if (item.getSubjectCategory() == QuotaSubjectCategory.TEAM) {
                teamResourceUsage = item;
            }
        }

        if (userResourceUsage == null)
            throw new QuotaAllocationException("userResourceUsage is null!");
        if (teamResourceUsage == null)
            throw new QuotaAllocationException("teamResourceUsage is null!");

        return Pair.of(userResourceUsage, teamResourceUsage);
    }
}
