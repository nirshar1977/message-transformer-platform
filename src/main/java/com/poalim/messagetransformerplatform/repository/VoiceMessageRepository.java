package com.poalim.messagetransformerplatform.repository;

import com.poalim.messagetransformerplatform.model.MessageStatus;
import com.poalim.messagetransformerplatform.model.VoiceMessage;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface VoiceMessageRepository extends MongoRepository<VoiceMessage, String> {

    List<VoiceMessage> findByStatus(MessageStatus status);

    List<VoiceMessage> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    List<VoiceMessage> findByRequestedBy(String requestedBy);
}