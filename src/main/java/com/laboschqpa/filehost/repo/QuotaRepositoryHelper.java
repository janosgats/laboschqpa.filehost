package com.laboschqpa.filehost.repo;

import com.laboschqpa.filehost.enums.IndexedFileStatus;
import com.laboschqpa.filehost.enums.QuotaSubjectCategory;
import com.laboschqpa.filehost.enums.attributeconverter.IndexedFileStatusAttributeConverter;
import com.laboschqpa.filehost.enums.attributeconverter.QuotaSubjectCategoryAttributeConverter;

import java.util.List;
import java.util.stream.Collectors;

public class QuotaRepositoryHelper {
    private static final IndexedFileStatusAttributeConverter indexedFileStatusAttributeConverter = new IndexedFileStatusAttributeConverter();
    private static final QuotaSubjectCategoryAttributeConverter quotaSubjectCategoryAttributeConverter = new QuotaSubjectCategoryAttributeConverter();

    public static List<Integer> statusInClauseContent() {
        final List<IndexedFileStatus> indexedFileStatusesCountedIntoQuota = List.of(
                IndexedFileStatus.ADDED_TO_DATABASE_INDEX,
                IndexedFileStatus.PRE_UPLOAD_PROCESSING,
                IndexedFileStatus.UPLOADING,
                IndexedFileStatus.UPLOAD_STREAM_SAVED,
                IndexedFileStatus.AVAILABLE,
                IndexedFileStatus.FAILED,
                IndexedFileStatus.ABORTED_BY_FILE_HOST,
                IndexedFileStatus.FAILED_DURING_DELETION
        );

        return indexedFileStatusesCountedIntoQuota.stream()
                .map(indexedFileStatusAttributeConverter::convertToDatabaseColumn)
                .collect(Collectors.toList());
    }

    public static Integer enumValOfUser() {
        return quotaSubjectCategoryAttributeConverter.convertToDatabaseColumn(QuotaSubjectCategory.USER);
    }

    public static Integer enumValOfTeam() {
        return quotaSubjectCategoryAttributeConverter.convertToDatabaseColumn(QuotaSubjectCategory.TEAM);
    }
}