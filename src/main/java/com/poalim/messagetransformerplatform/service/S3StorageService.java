package com.poalim.messagetransformerplatform.service;

import com.poalim.messagetransformerplatform.config.AwsS3Properties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.net.URL;
import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class S3StorageService {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final AwsS3Properties s3Properties;

    /**
     * Upload audio content to S3 bucket
     *
     * @param audioContent The audio file content
     * @param contentType The MIME type of the audio file
     * @return The S3 object key where the file was stored
     */
    public String uploadAudioToS3(byte[] audioContent, String contentType) {
        String objectKey = generateObjectKey();

        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(s3Properties.getBucketName())
                    .key(objectKey)
                    .contentType(contentType)
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromBytes(audioContent));
            log.info("Successfully uploaded audio file to S3: bucket={}, key={}, size={} bytes",
                    s3Properties.getBucketName(), objectKey, audioContent.length);

            return objectKey;
        } catch (Exception e) {
            log.error("Failed to upload audio file to S3", e);
            throw new RuntimeException("Failed to upload audio file to S3: " + e.getMessage(), e);
        }
    }

    /**
     * Generate a pre-signed URL for accessing the audio file
     *
     * @param objectKey The S3 object key
     * @param expirationDuration How long the URL should be valid
     * @return A pre-signed URL for the audio file
     */
    public URL generatePresignedUrl(String objectKey, Duration expirationDuration) {
        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(s3Properties.getBucketName())
                    .key(objectKey)
                    .build();

            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(expirationDuration)
                    .getObjectRequest(getObjectRequest)
                    .build();

            PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(presignRequest);
            URL presignedUrl = presignedRequest.url();

            log.info("Generated presigned URL for object: {} (expires in {} minutes)",
                    objectKey, expirationDuration.toMinutes());

            return presignedUrl;
        } catch (Exception e) {
            log.error("Failed to generate presigned URL for object: {}", objectKey, e);
            throw new RuntimeException("Failed to generate presigned URL: " + e.getMessage(), e);
        }
    }

    /**
     * Download audio content from S3
     *
     * @param objectKey The S3 object key
     * @return The audio content as a byte array
     */
    public byte[] downloadAudioFromS3(String objectKey) {
        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(s3Properties.getBucketName())
                    .key(objectKey)
                    .build();

            byte[] content = s3Client.getObjectAsBytes(getObjectRequest).asByteArray();
            log.info("Successfully downloaded audio file from S3: bucket={}, key={}, size={} bytes",
                    s3Properties.getBucketName(), objectKey, content.length);

            return content;
        } catch (Exception e) {
            log.error("Failed to download audio file from S3: {}", objectKey, e);
            throw new RuntimeException("Failed to download audio file from S3: " + e.getMessage(), e);
        }
    }

    /**
     * Generate a unique object key for an audio file
     *
     * @return A unique object key
     */
    private String generateObjectKey() {
        return "audio/" + UUID.randomUUID() + ".mp3";
    }
}