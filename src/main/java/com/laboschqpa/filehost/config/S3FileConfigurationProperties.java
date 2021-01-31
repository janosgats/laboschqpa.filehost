package com.laboschqpa.filehost.config;

import com.laboschqpa.filehost.enums.S3Provider;
import lombok.Data;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "filehost.s3file")
public class S3FileConfigurationProperties {
    private String uploadAccessKeyId;
    private String uploadAccessKeySecret;
    private String presignAccessKeyId;
    private String presignAccessKeySecret;

    private S3ProviderInput provider;
    private String region;
    private String bucket;
    private String objectKeyPrefix = "fileHostUpload_";

    public S3Provider getS3Provider() {
        return provider.getS3Provider();
    }

    public enum S3ProviderInput {
        AMAZON("amazon"),
        SCALE_WAY("scaleWay");

        @Getter
        private String value;

        S3ProviderInput(String value) {
            this.value = value;
        }

        S3Provider getS3Provider() {
            switch (this) {
                case AMAZON:
                    return S3Provider.AMAZON;
                case SCALE_WAY:
                    return S3Provider.SCALE_WAY;
            }

            throw new UnsupportedOperationException("No S3Provider mapped for S3ProviderInput: " + this.value);
        }
    }
}
