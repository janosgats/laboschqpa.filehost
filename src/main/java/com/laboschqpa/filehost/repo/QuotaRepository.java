package com.laboschqpa.filehost.repo;

import com.laboschqpa.filehost.entity.Quota;
import com.laboschqpa.filehost.enums.IndexedFileStatus;
import com.laboschqpa.filehost.enums.QuotaSubjectCategory;
import com.laboschqpa.filehost.enums.attributeconverter.IndexedFileStatusAttributeConverter;
import com.laboschqpa.filehost.enums.attributeconverter.QuotaSubjectCategoryAttributeConverter;
import com.laboschqpa.filehost.repo.dto.QuotaResourceUsageJpaDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface QuotaRepository extends JpaRepository<Quota, Long> {
    IndexedFileStatusAttributeConverter indexedFileStatusAttributeConverter = new IndexedFileStatusAttributeConverter();
    QuotaSubjectCategoryAttributeConverter quotaSubjectCategoryAttributeConverter = new QuotaSubjectCategoryAttributeConverter();

    @Query(value = "select * " +
            "from (select owner_user_id as subjectId, :categoryEnumVal_USER as subjectCategoryVal, coalesce(sum(stored_file.size), 0) as usedBytes " +
            "      from stored_file " +
            "               join indexed_file on stored_file.id = indexed_file.id " +
            "      where status in (:statusEnumVal_ADDED_TO_DATABASE_INDEX, :statusEnumVal_UPLOADING, :statusEnumVal_UPLOADED, :statusEnumVal_AVAILABLE, :statusEnumVal_FAILED, :statusEnumVal_ABORTED_BY_FILE_HOST, :statusEnumVal_FAILED_DURING_DELETION) " +
            "        and owner_user_id = :ownerUserId " +
            "     ) as user " +
            "UNION ALL " +
            "select * " +
            "from (select owner_team_id as subjectId, :categoryEnumVal_TEAM as subjectCategoryVal, coalesce(sum(stored_file.size), 0) as usedBytes " +
            "      from stored_file " +
            "               join indexed_file on stored_file.id = indexed_file.id " +
            "      where status in (:statusEnumVal_ADDED_TO_DATABASE_INDEX, :statusEnumVal_UPLOADING, :statusEnumVal_UPLOADED, :statusEnumVal_AVAILABLE, :statusEnumVal_FAILED, :statusEnumVal_ABORTED_BY_FILE_HOST) " +
            "        and owner_team_id = :ownerTeamId " +
            "     ) as team;",
            nativeQuery = true)
    List<QuotaResourceUsageJpaDto> getResourceQuotaUsageInternal(
            @Param("ownerUserId") long ownerUserId,
            @Param("ownerTeamId") long ownerTeamId,

            @Param("statusEnumVal_ADDED_TO_DATABASE_INDEX") int statusEnumVal_ADDED_TO_DATABASE_INDEX,
            @Param("statusEnumVal_UPLOADING") int statusEnumVal_UPLOADING,
            @Param("statusEnumVal_UPLOADED") int statusEnumVal_UPLOADED,
            @Param("statusEnumVal_AVAILABLE") int statusEnumVal_AVAILABLE,
            @Param("statusEnumVal_FAILED") int statusEnumVal_FAILED,
            @Param("statusEnumVal_ABORTED_BY_FILE_HOST") int statusEnumVal_ABORTED_BY_FILE_HOST,
            @Param("statusEnumVal_FAILED_DURING_DELETION") int statusEnumVal_FAILED_DURING_DELETION,

            @Param("categoryEnumVal_USER") int categoryEnumVal_USER,
            @Param("categoryEnumVal_TEAM") int categoryEnumVal_TEAM
    );

    default List<QuotaResourceUsageJpaDto> getResourceQuotaUsage(long ownerUserId, long ownerTeamId) {
        //An ugly way to pass in enum values for a nativeQuery
        return getResourceQuotaUsageInternal(
                ownerUserId,
                ownerTeamId,

                indexedFileStatusAttributeConverter.convertToDatabaseColumn(IndexedFileStatus.ADDED_TO_DATABASE_INDEX),
                indexedFileStatusAttributeConverter.convertToDatabaseColumn(IndexedFileStatus.UPLOADING),
                indexedFileStatusAttributeConverter.convertToDatabaseColumn(IndexedFileStatus.UPLOADED),
                indexedFileStatusAttributeConverter.convertToDatabaseColumn(IndexedFileStatus.AVAILABLE),
                indexedFileStatusAttributeConverter.convertToDatabaseColumn(IndexedFileStatus.FAILED),
                indexedFileStatusAttributeConverter.convertToDatabaseColumn(IndexedFileStatus.ABORTED_BY_FILE_HOST),
                indexedFileStatusAttributeConverter.convertToDatabaseColumn(IndexedFileStatus.FAILED_DURING_DELETION),

                quotaSubjectCategoryAttributeConverter.convertToDatabaseColumn(QuotaSubjectCategory.USER),
                quotaSubjectCategoryAttributeConverter.convertToDatabaseColumn(QuotaSubjectCategory.TEAM)
        );
    }

    Optional<Quota> findBySubjectCategoryAndSubjectId(QuotaSubjectCategory quotaSubjectCategory, long subjectId);

}