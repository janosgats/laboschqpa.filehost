package com.laboschcst.filehost.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@EnableAspectJAutoProxy
public class AppConfig {
    @Bean
    public WebClient smmPerformerWebClient() {
        return WebClient.builder().build();
    }
}
