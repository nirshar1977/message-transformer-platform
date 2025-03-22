package com.poalim.messagetransformerplatform.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VoiceProcessingStatus {

    private String messageId;
    private MessageStatus status;
    private LocalDateTime timestamp;
    private String errorMessage;

    // Additional fields that might be useful
    private String s3BucketName;
    private String s3ObjectKey;
}