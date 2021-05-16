package com.laboschqpa.filehost.repo;

import com.laboschqpa.filehost.entity.ImageVariant;
import com.laboschqpa.filehost.enums.ImageVariantStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import javax.persistence.LockModeType;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ImageVariantRepository extends JpaRepository<ImageVariant, Long> {

    @Query("select v.jobId from ImageVariant v " +
            " where " +
            "       v.status in :statusesForInstantPickup " +
            "    or (v.status in :failedStatuses and  v.statusUpdated < :pickUpFailedJobsBefore)" +
            "    or (v.status in :unfinishedStatuses and  v.statusUpdated < :pickUpUnfinishedJobsBefore  )" +
            " order by v.trialsCount asc " +
            " ")
    List<Long> getJobsIdsToPickUp(
            @Param("statusesForInstantPickup") Collection<ImageVariantStatus> statusesForInstantPickup,
            @Param("failedStatuses") Collection<ImageVariantStatus> failedStatuses,
            @Param("pickUpFailedJobsBefore") Instant pickUpFailedJobsBefore,
            @Param("unfinishedStatuses") Collection<ImageVariantStatus> unfinishedStatuses,
            @Param("pickUpUnfinishedJobsBefore") Instant pickUpUnfinishedJobsBefore,
            Pageable pageable);


    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select v, v.originalFile from ImageVariant v where v.jobId = :id")
    Optional<ImageVariant> findById_withEagerOriginalFile_withPessimisticWriteLock(@Param("id") long id);

    @Modifying
    @Query("update ImageVariant v set v.status = :newStatus, v.statusUpdated = :statusUpdated where v.jobId = :id")
    void updateStatus(@Param("id") long id, @Param("newStatus") ImageVariantStatus newStatus,
                      @Param("statusUpdated") Instant statusUpdated);
}
