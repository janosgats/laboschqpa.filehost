package com.laboschqpa.filehost.repo;

import com.laboschqpa.filehost.enums.IndexedFileStatus;
import com.laboschqpa.filehost.enums.UploadType;
import com.laboschqpa.filehost.enums.attributeconverter.IndexedFileStatusAttributeConverter;
import com.laboschqpa.filehost.enums.attributeconverter.UploadTypeAttributeConverter;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Log4j2
@RequiredArgsConstructor
@Service
public class ImageVariantJdbcRepository {
    private static final int UPLOAD_TYPE_IMAGE_VARIANT
            = new UploadTypeAttributeConverter().convertToDatabaseColumn(UploadType.IMAGE_VARIANT);
    private static final int INDEXED_FILE_STATUS_AVAILABLE
            = new IndexedFileStatusAttributeConverter().convertToDatabaseColumn(IndexedFileStatus.AVAILABLE);

    private final JdbcTemplate jdbcTemplate;

    public List<Long> listImageIdsWithoutVariantOfSizeToCreateVariantsFor(int variantSize, int limit) {
        return jdbcTemplate.queryForList(
                "select f.id from indexed_file f " +
                        " left join image_variant v " +
                        "       on f.id = v.original_file_id " +
                        "         and v.variant_size = ?" +
                        " where " +
                        "        f.status = " + INDEXED_FILE_STATUS_AVAILABLE + " " +
                        "     and f.is_image = TRUE " +
                        "     and f.upload_type <> " + UPLOAD_TYPE_IMAGE_VARIANT + " " +
                        "     and v.job_id is null " +
                        " limit ?"
                ,
                Long.class,
                variantSize,
                limit
        );
    }
}
