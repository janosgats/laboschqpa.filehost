package com.laboschqpa.filehost.repo;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Log4j2
@RequiredArgsConstructor
@Service
public class ImageVariantJdbcRepository {

    private final JdbcTemplate jdbcTemplate;

    public List<Long> listImageIdsWithoutVariantOfSize(int variantSize, int limit) {
        return jdbcTemplate.queryForList(
                "select f.id from indexed_file f " +
                        " left join image_variant v " +
                        "       on f.id = v.original_file_id " +
                        "         and v.variant_size = ?" +
                        " where f.is_image = TRUE " +
                        "     and v.job_id is null " +
                        " limit ?"
                ,
                Long.class,
                variantSize,
                limit
        );
    }
}
