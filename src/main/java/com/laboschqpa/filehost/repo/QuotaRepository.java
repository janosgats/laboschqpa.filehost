package com.laboschqpa.filehost.repo;

import com.laboschqpa.filehost.entity.Quota;
import com.laboschqpa.filehost.enums.QuotaSubjectCategory;
import com.laboschqpa.filehost.repo.dto.QuotaResourceUsageJpaDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface QuotaRepository extends JpaRepository<Quota, Long> {
    @Query(value = "select * " +
            "from (select owner_user_id as subjectId, :#{T(com.laboschqpa.filehost.repo.QuotaRepositoryHelper).enumValOfUser()} as subjectCategoryVal, coalesce(sum(indexed_file.size), 0) as usedBytes " +
            "      from indexed_file " +
            "      where status in :#{T(com.laboschqpa.filehost.repo.QuotaRepositoryHelper).statusInClauseContent()} " +
            "        and owner_user_id = :#{#ownerUserId} " +
            "     ) as user " +
            "UNION ALL " +
            "select * " +
            "from (select owner_team_id as subjectId, :#{T(com.laboschqpa.filehost.repo.QuotaRepositoryHelper).enumValOfTeam()}  as subjectCategoryVal, coalesce(sum(indexed_file.size), 0) as usedBytes " +
            "      from indexed_file " +
            "      where status in :#{T(com.laboschqpa.filehost.repo.QuotaRepositoryHelper).statusInClauseContent()} " +
            "        and owner_team_id = :#{#ownerTeamId} " +
            "     ) as team;",
            nativeQuery = true)
    List<QuotaResourceUsageJpaDto> getResourceQuotaUsage(@Param("ownerUserId") long ownerUserId, @Param("ownerTeamId") long ownerTeamId);

    Optional<Quota> findBySubjectCategoryAndSubjectId(QuotaSubjectCategory quotaSubjectCategory, long subjectId);

}