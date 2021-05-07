package com.laboschqpa.filehost.health;

import com.laboschqpa.filehost.config.AppConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping(AppConstants.healthControllerUrl)
public class HealthController {
    @GetMapping(AppConstants.healthPingSubUrl)
    public String getPing() {
        return "OK";
    }
}
