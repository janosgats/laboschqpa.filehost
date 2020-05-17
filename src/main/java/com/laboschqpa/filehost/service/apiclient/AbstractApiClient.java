package com.laboschqpa.filehost.service.apiclient;

public abstract class AbstractApiClient {
    protected ApiCallerFactory remoteAccountApiCallerFactory;

    private ApiCaller remoteAccountApiCaller;

    protected AbstractApiClient(ApiCallerFactory remoteAccountApiCallerFactory) {
        this.remoteAccountApiCallerFactory = remoteAccountApiCallerFactory;
    }

    /**
     * Using this method to instantiate the {@link ApiCaller} so the @Value private fields can be set before they are required at the ApiCaller instantiation.
     */
    protected ApiCaller getRemoteAccountApiCaller() {
        if (remoteAccountApiCaller == null) {
            remoteAccountApiCaller = remoteAccountApiCallerFactory.create(getApiBaseUrl());
        }
        return remoteAccountApiCaller;
    }

    protected abstract String getApiBaseUrl();
}
