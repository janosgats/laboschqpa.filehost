package com.laboschqpa.filehost.config;

public class AppConstants {
    public static final String userAccessibleBaseUrl = "/api/exposed";
    public static final String userAccessibleBaseUrlAntPattern = userAccessibleBaseUrl + "/**";

    public static final String prometheusMetricsExposeUrl = "/actuator/prometheus";

    public static final String healthControllerUrl = "/health";
    public static final String healthPingSubUrl = "/ping";
    public static final String healthPingUrlAntPattern = healthControllerUrl + healthPingSubUrl;

    public static final String internalNoAuthBaseUrl = "/api/internal/noAuth";
    public static final String internalNoAuthBaseUrlAntPattern = "/api/internal/noAuth/**";
}
