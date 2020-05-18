package com.laboschqpa.filehost.config;

import org.apache.tika.config.TikaConfig;
import org.apache.tika.detect.Detector;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@EnableAspectJAutoProxy
public class AppConfig {
    @Bean
    public WebClient webClient() {
        return WebClient.builder().build();
    }

    @Bean
    public Detector tikaDetector() {
        return TikaConfig.getDefaultConfig().getDetector();
    }
}
