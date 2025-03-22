package com.poalim.messagetransformerplatform.service;

import com.poalim.messagetransformerplatform.model.VoiceProcessingStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaProducerService {

    private final KafkaTemplate<String, VoiceProcessingStatus> kafkaTemplate;

    @Value("${kafka.topics.voice-processing-status}")
    private String voiceProcessingStatusTopic;

    /**
     * Publish a voice processing status update to Kafka
     *
     * @param status The voice processing status update
     * @return A completable future for the send operation
     */
    public CompletableFuture<SendResult<String, VoiceProcessingStatus>> publishStatusUpdate(VoiceProcessingStatus status) {
        log.info("Publishing voice processing status update to Kafka: {}", status);

        return kafkaTemplate.send(voiceProcessingStatusTopic, status.getMessageId(), status)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("Successfully published status update to Kafka for message: {}", status.getMessageId());
                    } else {
                        log.error("Failed to publish status update to Kafka for message: {}", status.getMessageId(), ex);
                    }
                });
    }
}