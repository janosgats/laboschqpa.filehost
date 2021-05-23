package com.laboschqpa.filehost.service.apiclient.imageconverter;

import com.laboschqpa.filehost.service.apiclient.AbstractApiClient;
import com.laboschqpa.filehost.service.apiclient.ApiCallerFactory;
import com.laboschqpa.filehost.service.apiclient.imageconverter.dto.ProcessCreationJobRequestDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import reactor.core.publisher.Mono;

@Service
public class ImageConverterApiClient extends AbstractApiClient {

    @Value("${apiClient.imageConverter.baseUrl}")
    private String apiBaseUrl;

    @Value("${apiClient.imageConverter.processImageVariantCreationJobUrl}")
    private String processImageVariantCreationJobUrl;

    public ImageConverterApiClient(ApiCallerFactory apiCallerFactory) {
        super(apiCallerFactory, true);
    }

    public Mono<Void> processImageVariantCreationJob(ProcessCreationJobRequestDto requestDto) {
        return getApiCaller().doCallAndThrowExceptionIfStatuscodeIsNot2xx(
                Void.class,
                processImageVariantCreationJobUrl,
                HttpMethod.POST,
                BodyInserters.fromValue(requestDto)
        );
    }


    @Override
    protected String getApiBaseUrl() {
        return apiBaseUrl;
    }
}
