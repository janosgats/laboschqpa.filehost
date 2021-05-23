package com.laboschqpa.filehost.api.controller.internal.noauth;

import com.laboschqpa.filehost.config.AppConstants;
import com.laboschqpa.filehost.service.imagevariant.VariantCreatorService;
import com.laboschqpa.filehost.service.imagevariant.VariantJobPickupService;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Log4j2
@RequiredArgsConstructor
@RestController
@RequestMapping(AppConstants.internalNoAuthBaseUrl + "/imageVariant")
public class InternalNoAuthImageVariantController {
    private final VariantCreatorService variantCreatorService;
    private final VariantJobPickupService variantJobPickupService;

    @PostMapping("/createSomeMissingVariants")
    public void postCreateSomeMissingVariants() {
        variantCreatorService.createSomeMissingVariants();
        log.trace("imageVariant/createMissingVariants endpoint ran successfully");
    }

    @ApiOperation("Picks up A FEW jobs. Should be called frequently, periodically.")
    @PostMapping("/pickUpSomeCreationJobs")
    public void postPickUpSomeCreationJobs() {
        variantJobPickupService.pickUpSomeCreationJobs();
    }

}
