package com.laboschqpa.filehost.model.file;

import com.laboschqpa.filehost.entity.S3FileEntity;
import com.laboschqpa.filehost.repo.S3FileEntityRepository;
import com.laboschqpa.filehost.service.S3FileSaver;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import javax.servlet.http.HttpServletRequest;
import java.io.InputStream;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;

@Log4j2
public class S3File extends AbstractIndexedFile<S3FileEntity> implements UploadableFile, HttpServableFile, DeletableFile {
    private static final long PRESIGNED_URL_EXPIRATION_SECONDS = 90;
    private static final long PRESIGNED_URL_REFRESH_THRESHOLD_SECONDS = 10;

    private final S3FileEntityRepository s3FileEntityRepository;
    private final S3FileSaver s3FileSaver;
    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    public S3File(S3FileEntity s3FileEntity, S3FileSaver s3FileSaver, S3Client s3Client, S3Presigner s3Presigner, S3FileEntityRepository s3FileEntityRepository) {
        super(s3FileEntity);

        this.s3FileSaver = s3FileSaver;
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
        this.s3FileEntityRepository = s3FileEntityRepository;
    }

    @Override
    public void saveFromStream(InputStream fileUploadingInputStream) {
        s3FileSaver.writeFromStream(fileUploadingInputStream, indexedFileEntity);
    }

    @Override
    public void cleanUpFailedUpload() {
        // Nothing to do here
    }

    @Override
    public void delete() {
        final DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                .bucket(indexedFileEntity.getBucket())
                .key(indexedFileEntity.getObjectKey())
                .build();

        s3Client.deleteObject(deleteObjectRequest);
    }

    @Override
    public ResponseEntity<Resource> getDownloadResponseEntity(HttpServletRequest request) {
        final String presignedUrl = getFreshPresignedUrl();

        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(presignedUrl))
                .build();
    }

    String getFreshPresignedUrl() {
        if (shouldPresignedUrlBeRefreshed()) {
            final GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(indexedFileEntity.getBucket())
                    .key(indexedFileEntity.getObjectKey())
                    .build();

            final GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofSeconds(PRESIGNED_URL_EXPIRATION_SECONDS))
                    .getObjectRequest(getObjectRequest)
                    .build();

            final PresignedGetObjectRequest presignedGetObjectRequest = s3Presigner.presignGetObject(presignRequest);

            indexedFileEntity.setCachedPresignedUrl(presignedGetObjectRequest.url().toString());
            indexedFileEntity.setPresignedUrlExpiration(presignedGetObjectRequest.expiration());
            s3FileEntityRepository.save(indexedFileEntity);
        }

        return indexedFileEntity.getCachedPresignedUrl();
    }

    boolean shouldPresignedUrlBeRefreshed() {
        final Instant expirationLimit = Instant.now().plusSeconds(PRESIGNED_URL_REFRESH_THRESHOLD_SECONDS);

        return StringUtils.isBlank(indexedFileEntity.getCachedPresignedUrl())
                || indexedFileEntity.getPresignedUrlExpiration() == null
                || indexedFileEntity.getPresignedUrlExpiration().isBefore(expirationLimit);
    }

}
