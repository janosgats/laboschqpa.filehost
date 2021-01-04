package com.laboschqpa.filehost.service.apiclient;

import com.laboschqpa.filehost.service.authinterservice.AuthInterServiceCrypto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
@RequiredArgsConstructor
public class ApiCallerFactory {
    private final WebClient webClient;
    private final AuthInterServiceCrypto authInterServiceCrypto;

    public ApiCaller createGeneral(String apiBaseUrl) {
        return new ApiCaller(apiBaseUrl, webClient, new String[0]);
    }

    public ApiCaller createForAuthInterService(String apiBaseUrl) {
        return new ApiCaller(apiBaseUrl, webClient, new String[0], authInterServiceCrypto);
    }
}
