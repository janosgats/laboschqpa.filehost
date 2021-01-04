package com.laboschqpa.filehost.service.apiclient;

public abstract class AbstractApiClient {
    private final ApiCallerFactory apiCallerFactory;
    private final boolean useAuthInterService;

    private ApiCaller apiCaller;

    public AbstractApiClient(ApiCallerFactory apiCallerFactory, boolean useAuthInterService) {
        this.apiCallerFactory = apiCallerFactory;
        this.useAuthInterService = useAuthInterService;
    }

    /**
     * Use this method to instantiate the {@link ApiCaller} so the @Value private fields can be set before they are required at the ApiCaller instantiation.
     */
    protected ApiCaller getApiCaller() {
        if (apiCaller == null) {
            apiCaller = instantiateApiCaller();
        }
        return apiCaller;
    }

    private ApiCaller instantiateApiCaller() {
        if (useAuthInterService) {
            return apiCallerFactory.createForAuthInterService(getApiBaseUrl());
        } else {
            return apiCallerFactory.createGeneral(getApiBaseUrl());
        }
    }

    protected abstract String getApiBaseUrl();
}
