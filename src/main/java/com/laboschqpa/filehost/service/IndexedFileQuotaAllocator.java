package com.laboschqpa.filehost.service;

import com.laboschqpa.filehost.entity.IndexedFileEntity;
import com.laboschqpa.filehost.entity.Quota;
import com.laboschqpa.filehost.enums.QuotaSubjectCategory;
import com.laboschqpa.filehost.enums.apierrordescriptor.QuotaExceededApiError;
import com.laboschqpa.filehost.enums.apierrordescriptor.UploadApiError;
import com.laboschqpa.filehost.exceptions.apierrordescriptor.QuotaExceededException;
import com.laboschqpa.filehost.exceptions.apierrordescriptor.UploadException;
import com.laboschqpa.filehost.repo.IndexedFileEntityRepository;
import com.laboschqpa.filehost.repo.QuotaRepository;
import com.laboschqpa.filehost.repo.dto.QuotaResourceUsageJpaDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Log4j2
@RequiredArgsConstructor
@Service
public class IndexedFileQuotaAllocator {
    private static final int KB = 1000;//Bytes in a KiloByte
    private static final int MB = 1000 * 1000;//Bytes in a MegaByte

    private final QuotaRepository quotaRepository;
    private final IndexedFileEntityRepository indexedFileEntityRepository;

    @Value("${quota.default.user}")
    private Long defaultUserQuotaLimitBytes;
    @Value("${quota.default.team}")
    private Long defaultTeamQuotaLimitBytes;

    /**
     * @return All bytes allocated for the resource.
     * @throws QuotaExceededException if no quota left
     */
    public long allocateQuota(IndexedFileEntity indexedFileEntity, long bytesReadSoFar, Long approximateFileSize) {
        final Quota userQuota = getQuota(QuotaSubjectCategory.USER, indexedFileEntity.getOwnerUserId());
        final Quota teamQuota = getQuota(QuotaSubjectCategory.TEAM, indexedFileEntity.getOwnerTeamId());

        final Pair<QuotaResourceUsageJpaDto, QuotaResourceUsageJpaDto> resourceUsages = getQuotaResourceUsage(indexedFileEntity);
        final QuotaResourceUsageJpaDto userResourceUsageBefore = resourceUsages.getFirst();
        final QuotaResourceUsageJpaDto teamResourceUsageBefore = resourceUsages.getSecond();

        assertQuotaIsNotExceeded(userQuota, userResourceUsageBefore, teamQuota, teamResourceUsageBefore);

        final long userFreeBytes = userQuota.getLimitBytes() - userResourceUsageBefore.getUsedBytes();
        final long teamFreeBytes = teamQuota.getLimitBytes() - teamResourceUsageBefore.getUsedBytes();

        if (approximateFileSize != null && bytesReadSoFar < (approximateFileSize * 0.9d)) {
            long newBytesToAllocate = approximateFileSize - bytesReadSoFar;

            if (userFreeBytes < newBytesToAllocate) {
                throw new QuotaExceededException(QuotaExceededApiError.USER_QUOTA_EXCEEDED,
                        "Uploading the file would exceed user storage quota!");
            }
            if (teamFreeBytes < newBytesToAllocate) {
                throw new QuotaExceededException(QuotaExceededApiError.TEAM_QUOTA_EXCEEDED,
                        "Uploading the file would exceed team storage quota!");
            }

            long completeFileSize = bytesReadSoFar + newBytesToAllocate;
            allocateBytes(completeFileSize, indexedFileEntity, userQuota, teamQuota);
            return completeFileSize;
        } else if (approximateFileSize != null) {
            final long wantedNewBytes;
            if (approximateFileSize / (double) bytesReadSoFar < 2) {
                wantedNewBytes = (long) (approximateFileSize * 0.34d);
            } else {
                wantedNewBytes = (long) (bytesReadSoFar * 0.34d);
            }

            final long newBytesToAllocate = limitWantedBytesAccordingToFreeSpace(wantedNewBytes, userFreeBytes, teamFreeBytes);
            final long completeFileSize = bytesReadSoFar + newBytesToAllocate;
            allocateBytes(completeFileSize, indexedFileEntity, userQuota, teamQuota);
            return completeFileSize;
        }

        final long wantedNewBytes;
        if (bytesReadSoFar < 13 * MB) {
            wantedNewBytes = 5 * MB;
        } else {
            wantedNewBytes = (long) (bytesReadSoFar * 0.34d);
        }

        final long newBytesToAllocate = limitWantedBytesAccordingToFreeSpace(wantedNewBytes, userFreeBytes, teamFreeBytes);
        final long completeFileSize = bytesReadSoFar + newBytesToAllocate;
        allocateBytes(completeFileSize, indexedFileEntity, userQuota, teamQuota);
        return completeFileSize;

        //TODO: Eliminate the magic numbers!
    }

    private long limitWantedBytesAccordingToFreeSpace(long wantedNewBytes, long userFreeBytes, long teamFreeBytes) {
        final long userLimitedBytes = Math.min(wantedNewBytes, (long) (userFreeBytes * 0.99d));
        final long teamLimitedBytes = Math.min(wantedNewBytes, (long) (teamFreeBytes * 0.99d));

        if (userLimitedBytes < KB) {
            throw new QuotaExceededException(QuotaExceededApiError.USER_QUOTA_EXCEEDED,
                    "User storage quota is exceeded! Cannot allocate more space.");
        }
        if (teamLimitedBytes < KB) {
            throw new QuotaExceededException(QuotaExceededApiError.TEAM_QUOTA_EXCEEDED,
                    "Team storage quota is exceeded! Cannot allocate more space.");
        }

        return Math.min(userLimitedBytes, teamLimitedBytes);
    }

    private void allocateBytes(long completeFileSize, IndexedFileEntity indexedFileEntity, Quota userQuota, Quota teamQuota) {
        indexedFileEntity.setSize(completeFileSize);
        indexedFileEntityRepository.save(indexedFileEntity);

        final Pair<QuotaResourceUsageJpaDto, QuotaResourceUsageJpaDto> resourceUsages = getQuotaResourceUsage(indexedFileEntity);
        final QuotaResourceUsageJpaDto userResourceUsageBefore = resourceUsages.getFirst();
        final QuotaResourceUsageJpaDto teamResourceUsageBefore = resourceUsages.getSecond();

        assertQuotaIsNotExceeded(userQuota, userResourceUsageBefore, teamQuota, teamResourceUsageBefore);
        log.trace("Allocated storage for file: {}, user: {}, team: {}. completeFileSize: {}MB",
                indexedFileEntity.getId(), userQuota.getSubjectId(), teamQuota.getSubjectId(), (double) completeFileSize / MB);
    }

    private void assertQuotaIsNotExceeded(Quota userQuota, QuotaResourceUsageJpaDto userResourceUsage, Quota teamQuota, QuotaResourceUsageJpaDto teamResourceUsage) {
        if (userResourceUsage.getUsedBytes() > userQuota.getLimitBytes()) {
            throw new QuotaExceededException(QuotaExceededApiError.USER_QUOTA_EXCEEDED,
                    "User storage quota is exceeded at " + getUsagePercentage(userQuota, userResourceUsage) + " percents!");
        }
        if (teamResourceUsage.getUsedBytes() > teamQuota.getLimitBytes()) {
            throw new QuotaExceededException(QuotaExceededApiError.TEAM_QUOTA_EXCEEDED,
                    "Team storage quota is exceeded at " + getUsagePercentage(teamQuota, teamResourceUsage) + " percents!");
        }
    }

    private Integer getUsagePercentage(Quota quota, QuotaResourceUsageJpaDto usage) {
        return Math.round((float) usage.getUsedBytes() / (float) quota.getLimitBytes() * 100);
    }

    private Quota getQuota(QuotaSubjectCategory subjectCategory, Long subjectId) {
        Optional<Quota> quotaOptional = quotaRepository.findBySubjectCategoryAndSubjectId(subjectCategory, subjectId);

        return quotaOptional.orElseGet(() -> {
            final Quota quota = new Quota();
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
        });
    }

    private Pair<QuotaResourceUsageJpaDto, QuotaResourceUsageJpaDto> getQuotaResourceUsage(IndexedFileEntity indexedFileEntity) {
        List<QuotaResourceUsageJpaDto> resourceUsage
                = quotaRepository.getResourceQuotaUsage(indexedFileEntity.getOwnerUserId(), indexedFileEntity.getOwnerTeamId());

        QuotaResourceUsageJpaDto userResourceUsage = null, teamResourceUsage = null;
        for (QuotaResourceUsageJpaDto item : resourceUsage) {
            if (item.getSubjectCategory() == QuotaSubjectCategory.USER) {
                userResourceUsage = item;
            } else if (item.getSubjectCategory() == QuotaSubjectCategory.TEAM) {
                teamResourceUsage = item;
            }
        }

        if (userResourceUsage == null)
            throw new UploadException(UploadApiError.ERROR_DURING_QUOTA_ALLOCATION, "userResourceUsage is null!");
        if (teamResourceUsage == null)
            throw new UploadException(UploadApiError.ERROR_DURING_QUOTA_ALLOCATION, "teamResourceUsage is null!");

        return Pair.of(userResourceUsage, teamResourceUsage);
    }
}
