package com.laboschqpa.filehost.service.apiclient;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
@RequiredArgsConstructor
public class ApiCallerFactory {
    private final WebClient webClient;

    public ApiCaller create(String apiBaseUrl) {
        return new ApiCaller(apiBaseUrl, webClient);
    }
}
