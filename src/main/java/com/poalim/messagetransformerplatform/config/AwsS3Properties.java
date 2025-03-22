package com.poalim.messagetransformerplatform.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "aws.s3")
@Data
public class AwsS3Properties {

    private String bucketName;
    private String region;
    private String endpoint; // Used for local development with MinIO
    private boolean pathStyleAccessEnabled; // Used for local development with MinIO
}