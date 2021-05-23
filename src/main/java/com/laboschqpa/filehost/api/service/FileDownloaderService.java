package com.laboschqpa.filehost.api.service;

import com.laboschqpa.filehost.annotation.ExceptionWrappedFileServingClass;
import com.laboschqpa.filehost.entity.ImageVariant;
import com.laboschqpa.filehost.entity.IndexedFileEntity;
import com.laboschqpa.filehost.enums.apierrordescriptor.FileServingApiError;
import com.laboschqpa.filehost.exceptions.apierrordescriptor.ContentNotFoundException;
import com.laboschqpa.filehost.exceptions.apierrordescriptor.FileServingException;
import com.laboschqpa.filehost.model.download.FileDownloadRequest;
import com.laboschqpa.filehost.model.file.HttpServableFile;
import com.laboschqpa.filehost.model.file.factory.HttpServableFileFactory;
import com.laboschqpa.filehost.repo.ImageVariantRepository;
import com.laboschqpa.filehost.repo.IndexedFileEntityRepository;
import com.laboschqpa.filehost.util.ImageVariantSelector;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@Log4j2
@RequiredArgsConstructor
@Service
@ExceptionWrappedFileServingClass
public class FileDownloaderService {
    private static final int DEFAULT_OPTIMIZED_IMAGE_SIZE = 800;

    private final HttpServableFileFactory httpServableFileFactory;
    private final IndexedFileEntityRepository indexedFileEntityRepository;
    private final ImageVariantRepository imageVariantRepository;

    /**
     * Always serves the requested file. (Never tries to optimize and select a different file.)
     */
    public ResponseEntity<Resource> downloadOriginalFile(long indexedFileId, HttpServletRequest request) {
        final HttpServableFile httpServableFile = httpServableFileFactory.from(indexedFileId);

        if (!httpServableFile.isAvailable()) {
            throw new FileServingException(FileServingApiError.FILE_IS_NOT_AVAILABLE,
                    "The requested file is not available for download. File status: " + httpServableFile.getStatus());
        }

        return httpServableFile.getDownloadResponseEntity(request);
    }

    /**
     * Tries to select the optimal file to serve. It may be different than the requested one.
     * TODO: Optimize browser file caching
     */
    public ResponseEntity<Resource> downloadOptimalFile(FileDownloadRequest downloadRequest, HttpServletRequest request) {
        final IndexedFileEntity fileEntityToServe = getOptimalIndexedFileToServe(downloadRequest);

        final HttpServableFile httpServableFile = httpServableFileFactory.fromIndexedFileEntity(fileEntityToServe);

        if (!httpServableFile.isAvailable()) {
            throw new FileServingException(FileServingApiError.FILE_IS_NOT_AVAILABLE,
                    "The requested file is not available for download. File status: " + httpServableFile.getStatus());
        }

        log.debug("DownloadOptimal: Requested file: {}, wantedSize: {}, Served file: {}",
                downloadRequest.getFileId(), downloadRequest.getWantedImageSize(), httpServableFile.getEntity().getId());
        return httpServableFile.getDownloadResponseEntity(request);
    }

    private IndexedFileEntity getOptimalIndexedFileToServe(FileDownloadRequest downloadRequest) {
        final IndexedFileEntity originalFileEntity = getExistingIndexedFileEntity(downloadRequest.getFileId());

        if (originalFileEntity.getIsImage() == null || !originalFileEntity.getIsImage()) {
            return originalFileEntity;
        }

        final int wantedImageSize = downloadRequest.getWantedImageSize() != null ? downloadRequest.getWantedImageSize()
                : DEFAULT_OPTIMIZED_IMAGE_SIZE;

        return getImageVariantFileToServe(originalFileEntity, wantedImageSize);
    }

    private IndexedFileEntity getImageVariantFileToServe(IndexedFileEntity originalFileEntity, int wantedImageSize) {
        final List<ImageVariant> variants = imageVariantRepository.findAllByOriginalFileId(originalFileEntity.getId());

        if (variants.isEmpty()) {
            return originalFileEntity;
        }

        final ImageVariant variantToServe = ImageVariantSelector.selectImageVariantToServe(variants, wantedImageSize);
        return getExistingIndexedFileEntity(variantToServe.getVariantFile().getId());
    }

    private IndexedFileEntity getExistingIndexedFileEntity(long id) {
        return indexedFileEntityRepository.findById(id)
                .orElseThrow(() -> new ContentNotFoundException("Cannot find IndexedFileEntity for download. id: " + id));
    }
}
