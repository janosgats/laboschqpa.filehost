package com.laboschqpa.filehost.config;

import com.laboschqpa.filehost.enums.S3Provider;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;
import java.net.URISyntaxException;

@RequiredArgsConstructor
@EnableConfigurationProperties
@Configuration
@EnableAspectJAutoProxy
public class S3Config {
    private final S3FileConfigurationProperties configurationProperties;

    @Bean
    public S3Presigner s3Presigner() throws URISyntaxException {
        final AwsBasicCredentials presignCredentials = AwsBasicCredentials.create(
                configurationProperties.getPresignAccessKeyId(),
                configurationProperties.getPresignAccessKeySecret()
        );

        return S3Presigner.builder()
                .region(Region.of(configurationProperties.getRegion()))
                .credentialsProvider(() -> presignCredentials)
                .endpointOverride(new URI(getEndpointUrl()))
                .build();
    }

    @Bean
    public S3Client s3Client() throws URISyntaxException {
        final AwsBasicCredentials uploadCredentials = AwsBasicCredentials.create(
                configurationProperties.getUploadAccessKeyId(),
                configurationProperties.getUploadAccessKeySecret()
        );

        return S3Client.builder()
                .region(Region.of(configurationProperties.getRegion()))
                .credentialsProvider(() -> uploadCredentials)
                .endpointOverride(new URI(getEndpointUrl()))
                .build();
    }

    private String getEndpointUrl() {
        final S3Provider s3Provider = configurationProperties.getProvider().getS3Provider();
        return "https://s3." + configurationProperties.getRegion() + "." + s3Provider.getEndpointDomain();
    }
}
