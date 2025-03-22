package com.poalim.messagetransformerplatform.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class TextToSpeechService {

    private final WebClient webClient;

    @Value("${llm.api.endpoint}")
    private String llmApiEndpoint;

    @Value("${llm.api.key}")
    private String llmApiKey;

    /**
     * Convert text to speech using the LLM API
     *
     * @param text The text to convert to speech
     * @return The audio content as a byte array
     */
    public Mono<byte[]> convertTextToSpeech(String text) {
        log.info("Converting text to speech: {}", text);

        // Build request payload for OpenAI API
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "tts-1");
        requestBody.put("input", text);
        requestBody.put("voice", "alloy");

        return webClient.post()
                .uri(llmApiEndpoint + "/audio/speech")
                .header("Authorization", "Bearer " + llmApiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(requestBody))
                .retrieve()
                .bodyToFlux(org.springframework.core.io.buffer.DataBuffer.class)
                .collectList()
                .map(dataBuffers -> {
                    try {
                        org.springframework.core.io.buffer.DataBuffer joinedBuffers =
                                DataBufferUtils.join((Publisher<? extends DataBuffer>) dataBuffers).block();
                        if (joinedBuffers != null) {
                            byte[] bytes = new byte[joinedBuffers.readableByteCount()];
                            joinedBuffers.read(bytes);
                            DataBufferUtils.release(joinedBuffers);
                            return bytes;
                        }
                        return new byte[0];
                    } catch (Exception e) {
                        log.error("Error processing audio response", e);
                        throw new RuntimeException("Failed to process audio response", e);
                    }
                });
    }
}