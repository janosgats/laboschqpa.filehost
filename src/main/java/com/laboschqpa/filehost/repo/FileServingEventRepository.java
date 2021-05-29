package com.laboschqpa.filehost.repo;

import com.laboschqpa.filehost.entity.FileServingEvent;
import com.laboschqpa.filehost.enums.FileServingEventKind;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;

public interface FileServingEventRepository extends JpaRepository<FileServingEvent, Long> {
    @Query("" +
            " select count(fse) " +
            " from FileServingEvent fse " +
            " where " +
            "     fse.eventKind = :eventKind " +
            "     and fse.requesterUserId = :requesterUserId" +
            "     and fse.time > :sinceTime")
    long countOfEventsSince(FileServingEventKind eventKind, Long requesterUserId, Instant sinceTime);
}
