package com.laboschqpa.filehost.service.imagevariant;

import com.laboschqpa.filehost.entity.IndexedFileEntity;
import com.laboschqpa.filehost.exceptions.apierrordescriptor.InvalidUploadRequestException;
import com.laboschqpa.filehost.service.imagevariant.command.SaveImageVariantCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.tomcat.util.http.fileupload.FileItemIterator;
import org.apache.tomcat.util.http.fileupload.FileItemStream;
import org.apache.tomcat.util.http.fileupload.servlet.ServletFileUpload;
import org.apache.tomcat.util.http.fileupload.util.Streams;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

@Log4j2
@RequiredArgsConstructor
@Service
public class ImageVariantService {
    private static final Long IMAGE_VARIANT_UPLOADER_USER_ID = -10L;
    private static final Long IMAGE_VARIANT_UPLOADER_TEAM_ID = -10L;
    private static final int[] VARIANT_SIZES = new int[]{250, 500, 2000};

    private final MissingVariantCreatorService missingVariantCreatorService;
    private final VariantJobPickupService variantJobPickupService;

    public void createMissingVariants() {
        Exception lastCaughtException = null;

        for (int currentSize : VARIANT_SIZES) {
            try {
                missingVariantCreatorService.createForSize(currentSize);
            } catch (Exception e) {
                log.error("Exception while Creating Missing Variants for variantSize: {}", currentSize, e);
                lastCaughtException = e;
            }
        }

        if (lastCaughtException != null) {
            throw new RuntimeException("Exception while Creating Missing Variants - " +
                    lastCaughtException.getClass().getName() + ": " + lastCaughtException.getMessage(),
                    lastCaughtException);
        }
    }

    public void pickUpCreationJobs() {
        variantJobPickupService.pickUpCreationJobs();
    }

    public IndexedFileEntity saveVariant(SaveImageVariantCommand command) {
        //TODO: write this function
        tempConsumeRequestToDispose(command.getHttpServletRequest());
        return new IndexedFileEntity();
    }


    /**
     * TODO: This function is temporary and useless. Remove it when implementing variant upload.
     */
    private void tempConsumeRequestToDispose(HttpServletRequest httpServletRequest) {
        final ServletFileUpload servletFileUpload = new ServletFileUpload();
        try {
            FileItemIterator iterator = servletFileUpload.getItemIterator(httpServletRequest);
            if (!iterator.hasNext()) {
                throw new InvalidUploadRequestException("No fields present in the multipart request!");
            }

            Long approximateFileSize = null;

            FileItemStream uploadedFile;

            FileItemStream firstItem = iterator.next();
            if (firstItem.isFormField() && "approximateFileSize".equals(firstItem.getFieldName())) {
                approximateFileSize = Long.parseLong(Streams.asString(firstItem.openStream()));
                uploadedFile = iterator.next();
            } else {
                uploadedFile = firstItem;
            }

            String fieldName = uploadedFile.getFieldName();
            if (uploadedFile.isFormField()) {
                throw new InvalidUploadRequestException("Unexpected multipart form field is present in HTTP body: " + fieldName);
            }

            log.info("readAllBytes length: {}", uploadedFile.openStream().readAllBytes().length);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
