package com.laboschqpa.filehost.repo.custom;

import com.laboschqpa.filehost.repo.dto.IndexedFileOnlyJpaDto;
import com.laboschqpa.filehost.repo.dto.IndexedFileOnlyJpaDtoImpl;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

@Repository
@Transactional(readOnly = true)
public class ExtendedIndexedFileEntityRepositoryImpl implements ExtendedIndexedFileEntityRepository {
    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<IndexedFileOnlyJpaDto> findOnlyFromIndexedFileTableByMultipleIds(Collection<Long> ids) {
        if (ids.size() == 0) {
            return new LinkedList<>();
        }

        final String joinedIds = StringUtils.join(ids, ",");

        final Query query = entityManager.createNativeQuery(
                "select " +
                        "    id as id, " +
                        "    dtype as dType, " +
                        "    status as statusVal, " +
                        "    owner_user_id as ownerUserId, " +
                        "    owner_team_id as ownerTeamId, " +
                        "    creation_time as creationTime, " +
                        "    mime_type as mimeType, " +
                        "    name as name, " +
                        "    size as size " +
                        "from indexed_file " +
                        "where id in (" + joinedIds + ")");


        return ((List<Object[]>) query.getResultList()).stream().map(
                (row) -> {
                    IndexedFileOnlyJpaDtoImpl dto = new IndexedFileOnlyJpaDtoImpl();

                    dto.setId(((BigInteger) row[0]).longValue());
                    dto.setDType(((Byte) row[1]).intValue());
                    dto.setStatusVal((Integer) row[2]);
                    dto.setOwnerUserId(((BigInteger) row[3]).longValue());
                    dto.setOwnerTeamId(((BigInteger) row[4]).longValue());
                    dto.setCreationTime(((Timestamp) row[5]).toInstant());
                    dto.setMimeType((String) row[6]);
                    dto.setName((String) row[7]);
                    dto.setSize(((BigInteger) row[8]).longValue());

                    return dto;
                }
        ).collect(Collectors.toList());
    }
}