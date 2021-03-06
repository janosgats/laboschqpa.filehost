package com.laboschqpa.filehost.config;

public class AppConstants {
    public static final String userAccessibleBaseUrl = "/api/exposed";
    public static final String userAccessibleBaseUrlAntPattern = userAccessibleBaseUrl + "/**";

    public static final String prometheusMetricsExposeUrl = "/actuator/prometheus";
}
