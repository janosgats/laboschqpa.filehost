package com.laboschqpa.filehost.api.controller.internal;

import com.laboschqpa.filehost.api.dto.FileUploadResponse;
import com.laboschqpa.filehost.entity.IndexedFileEntity;
import com.laboschqpa.filehost.enums.ImageVariantStatus;
import com.laboschqpa.filehost.repo.ImageVariantRepository;
import com.laboschqpa.filehost.service.imagevariant.VariantSaverService;
import com.laboschqpa.filehost.service.imagevariant.VariantService;
import com.laboschqpa.filehost.service.imagevariant.command.SaveImageVariantCommand;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.stream.Collectors;

@Log4j2
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/internal/imageVariant")
public class ImageVariantController {
    private final VariantSaverService variantSaverService;
    private final VariantService variantService;
    private final ImageVariantRepository imageVariantRepository;

    @ApiOperation("To upload the result of a variant creation job.")
    @PostMapping("/uploadVariant")
    public FileUploadResponse postUploadImage(@RequestParam("jobId") Long jobId, HttpServletRequest httpServletRequest) {
        final SaveImageVariantCommand command = new SaveImageVariantCommand();
        command.setJobId(jobId);
        command.setHttpServletRequest(httpServletRequest);

        IndexedFileEntity createdFile = variantSaverService.saveVariant(command);

        return new FileUploadResponse(createdFile.getId(), createdFile.getMimeType());
    }

    @PostMapping("/signalJobFailedInJobProcessor")
    public void postSignalFailedJobInJobProcessor(@RequestParam("jobId") Long jobId) {
        variantService.signalJobFailedInJobProcessor(jobId);
    }

    @PostMapping("/markVariantFileAsCorrupt")
    public void postMarkVariantFileAsCorrupt(@RequestParam("variantFileId") Long variantFileId) {
        variantService.markVariantAsCorrupt(variantFileId);
    }

    @PostMapping("/markVariantAsCorrupt")
    public void postMarkVariantAsCorrupt(@RequestParam("originalFileId") Long originalFileId, @RequestParam("variantSize") Integer variantSize) {
        variantService.markVariantAsCorrupt(originalFileId, variantSize);
    }

    @GetMapping("/listSucceededVariantIdsOfFile")
    public List<Long> getListSucceededVariantIdsOfFile(@RequestParam("originalFileId") Long originalFileId) {
        return imageVariantRepository.findAllByOriginalFileIdAndStatus(originalFileId, ImageVariantStatus.SUCCEEDED).stream()
                .map(imageVariant -> imageVariant.getVariantFile().getId())
                .collect(Collectors.toList());
    }
}
