package com.laboschqpa.filehost.api.controller.internal;

import com.laboschqpa.filehost.api.dto.FileUploadResponse;
import com.laboschqpa.filehost.entity.IndexedFileEntity;
import com.laboschqpa.filehost.service.imagevariant.VariantSaverService;
import com.laboschqpa.filehost.service.imagevariant.VariantCreatorService;
import com.laboschqpa.filehost.service.imagevariant.VariantJobPickupService;
import com.laboschqpa.filehost.service.imagevariant.command.SaveImageVariantCommand;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

@Log4j2
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/internal/imageVariant")
public class ImageVariantController {

    private final VariantCreatorService createMissingVariants;
    private final VariantJobPickupService variantJobPickupService;
    private final VariantSaverService variantSaverService;

    @PostMapping("/createSomeMissingVariants")
    public void postCreateSomeMissingVariants() {
        //TODO: Trigger something similar tho this when a new file with image mime type uploaded. (Create the variant jobs to the new file.)
        createMissingVariants.createSomeMissingVariants();
        log.trace("imageVariant/createMissingVariants endpoint ran successfully");
    }

    @ApiOperation("Picks up A FEW jobs. Should be called frequently, periodically.")
    @PostMapping("/pickUpSomeCreationJobs")
    public void postPickUpSomeCreationJobs() {
        variantJobPickupService.pickUpSomeCreationJobs();
    }

    @PostMapping("/uploadVariant")
    public FileUploadResponse postUploadImage(@RequestParam("jobId") Long jobId, HttpServletRequest httpServletRequest) {
        final SaveImageVariantCommand command = new SaveImageVariantCommand();
        command.setJobId(jobId);
        command.setHttpServletRequest(httpServletRequest);

        IndexedFileEntity createdFile = variantSaverService.saveVariant(command);

        return new FileUploadResponse(createdFile.getId(), createdFile.getMimeType());
    }
}
