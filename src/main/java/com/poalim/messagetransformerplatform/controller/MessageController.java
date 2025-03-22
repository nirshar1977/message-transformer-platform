package com.poalim.messagetransformerplatform.controller;

import com.poalim.messagetransformerplatform.model.VoiceMessage;
import com.poalim.messagetransformerplatform.service.MessageTransformerService;
import com.poalim.messagetransformerplatform.service.S3StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.net.URL;
import java.time.Duration;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageTransformerService messageTransformerService;
    private final S3StorageService s3StorageService;

    /**
     * Submit a new text message for conversion to speech
     *
     * @param request The text message request
     * @return The created voice message
     */
    @PostMapping("/text-to-speech")
    public Mono<ResponseEntity<VoiceMessage>> submitTextToSpeech(@RequestBody Map<String, String> request) {
        String text = request.get("text");
        String requestedBy = request.getOrDefault("requestedBy", "anonymous");

        return messageTransformerService.processTextMessage(text, requestedBy)
                .map(ResponseEntity::ok);
    }

    /**
     * Get a voice message by ID
     *
     * @param messageId The message ID
     * @return The voice message
     */
    @GetMapping("/{messageId}")
    public ResponseEntity<VoiceMessage> getVoiceMessage(@PathVariable String messageId) {
        VoiceMessage message = messageTransformerService.getVoiceMessage(messageId);
        return ResponseEntity.ok(message);
    }

    /**
     * Get a presigned URL for a voice message audio file
     *
     * @param messageId The message ID
     * @return The presigned URL
     */
    @GetMapping("/{messageId}/audio-url")
    public ResponseEntity<Map<String, String>> getAudioUrl(@PathVariable String messageId) {
        VoiceMessage message = messageTransformerService.getVoiceMessage(messageId);

        if (message.getS3ObjectKey() == null) {
            return ResponseEntity.badRequest().build();
        }

        URL presignedUrl = s3StorageService.generatePresignedUrl(
                message.getS3ObjectKey(), Duration.ofMinutes(15));

        return ResponseEntity.ok(Map.of("audioUrl", presignedUrl.toString()));
    }
}