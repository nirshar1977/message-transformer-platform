package com.poalim.messagetransformerplatform.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "voice_messages")
public class VoiceMessage {

    @Id
    private String id;

    private String originalText;
    private String s3BucketName;
    private String s3ObjectKey;
    
    public String getS3ObjectKey() {
        return s3ObjectKey;
    }
    private String contentType;
    private Long fileSizeBytes;

    private MessageStatus status;
    private String errorMessage;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime processedAt;

    // Additional metadata fields
    private String requestedBy;
    private String voiceType; // Could be used for different voice types if supported by LLM
}