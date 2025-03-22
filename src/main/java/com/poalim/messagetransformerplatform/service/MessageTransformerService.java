package com.poalim.messagetransformerplatform.service;

import com.poalim.messagetransformerplatform.model.MessageStatus;
import com.poalim.messagetransformerplatform.model.VoiceMessage;
import com.poalim.messagetransformerplatform.model.VoiceProcessingStatus;
import com.poalim.messagetransformerplatform.repository.VoiceMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MessageTransformerService {

    private final VoiceMessageRepository voiceMessageRepository;
    private final TextToSpeechService textToSpeechService;
    private final S3StorageService s3StorageService;
    private final KafkaProducerService kafkaProducerService;

    @Value("${aws.s3.bucket-name}")
    private String s3BucketName;

    /**
     * Process a text message by converting it to speech and storing the result
     *
     * @param text The text to convert to speech
     * @param requestedBy Who requested the conversion
     * @return The created voice message record
     */
    public Mono<VoiceMessage> processTextMessage(String text, String requestedBy) {
        log.info("Processing text message: {}", text);

        // Initialize a VoiceMessage with RECEIVED status
        VoiceMessage voiceMessage = VoiceMessage.builder()
                .id(UUID.randomUUID().toString())
                .originalText(text)
                .status(MessageStatus.RECEIVED)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .requestedBy(requestedBy)
                .build();

        // Save the initial message
        voiceMessage = voiceMessageRepository.save(voiceMessage);

        // Publish RECEIVED status
        publishStatusUpdate(voiceMessage);

        final String messageId = voiceMessage.getId();

        // Start text-to-speech conversion
        return updateMessageStatus(messageId, MessageStatus.PROCESSING)
                .flatMap(updatedMessage -> {
                    // Convert text to speech
                    return textToSpeechService.convertTextToSpeech(text)
                            .flatMap(audioContent -> {
                                try {
                                    // Upload to S3
                                    String objectKey = s3StorageService.uploadAudioToS3(
                                            audioContent, "audio/mpeg");

                                    // Update message with S3 location
                                    updatedMessage.setS3BucketName(s3BucketName);
                                    updatedMessage.setS3ObjectKey(objectKey);
                                    updatedMessage.setContentType("audio/mpeg");
                                    updatedMessage.setFileSizeBytes((long) audioContent.length);
                                    updatedMessage.setProcessedAt(LocalDateTime.now());
                                    updatedMessage.setStatus(MessageStatus.COMPLETED);
                                    updatedMessage.setUpdatedAt(LocalDateTime.now());

                                    // Save updated message
                                    VoiceMessage savedMessage = voiceMessageRepository.save(updatedMessage);

                                    // Publish COMPLETED status
                                    publishStatusUpdate(savedMessage);

                                    return Mono.just(savedMessage);
                                } catch (Exception e) {
                                    log.error("Error processing voice message", e);
                                    return handleProcessingError(updatedMessage, e);
                                }
                            })
                            .onErrorResume(e -> handleProcessingError(updatedMessage, e));
                });
    }

    /**
     * Update the status of a message
     *
     * @param messageId The message ID
     * @param newStatus The new status
     * @return The updated message
     */
    private Mono<VoiceMessage> updateMessageStatus(String messageId, MessageStatus newStatus) {
        VoiceMessage message = voiceMessageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message not found: " + messageId));

        message.setStatus(newStatus);
        message.setUpdatedAt(LocalDateTime.now());

        VoiceMessage updatedMessage = voiceMessageRepository.save(message);
        publishStatusUpdate(updatedMessage);

        return Mono.just(updatedMessage);
    }

    /**
     * Handle a processing error
     *
     * @param message The message that failed
     * @param e The exception that occurred
     * @return The updated message with FAILED status
     */
    private Mono<VoiceMessage> handleProcessingError(VoiceMessage message, Throwable e) {
        message.setStatus(MessageStatus.FAILED);
        message.setErrorMessage(e.getMessage());
        message.setUpdatedAt(LocalDateTime.now());

        VoiceMessage savedMessage = voiceMessageRepository.save(message);
        publishStatusUpdate(savedMessage);

        return Mono.just(savedMessage);
    }

    /**
     * Publish a status update to Kafka
     *
     * @param message The voice message
     */
    private void publishStatusUpdate(VoiceMessage message) {
        VoiceProcessingStatus status = VoiceProcessingStatus.builder()
                .messageId(message.getId())
                .status(message.getStatus())
                .timestamp(LocalDateTime.now())
                .errorMessage(message.getErrorMessage())
                .s3BucketName(message.getS3BucketName())
                .s3ObjectKey(message.getS3ObjectKey())
                .build();

        kafkaProducerService.publishStatusUpdate(status);
    }

    /**
     * Retrieve a voice message by ID
     *
     * @param messageId The message ID
     * @return The voice message
     */
    public VoiceMessage getVoiceMessage(String messageId) {
        return voiceMessageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Voice message not found: " + messageId));
    }
}