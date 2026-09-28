package com.example.tokens.events;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class TokenEventListener {

    private final ObjectMapper objectMapper;
    private final TokenEventHandler handler;

    public TokenEventListener(ObjectMapper objectMapper, TokenEventHandler handler) {
        this.objectMapper = objectMapper;
        this.handler = handler;
    }

    /**
     * Unique group id per JVM instance: each of the 3 replicas must see every event so it can evict its
     * own local {@link com.example.tokens.token.TokenCache} entry. A shared group id would let Kafka hand
     * a given event to only one replica, leaving the others serving a stale cached token until TTL expiry
     * (see README "Blocking (security requirement)" and the Task 2 incident).
     */
    @KafkaListener(topics = "${app.topics.token-events}",
            groupId = "token-authorization-service-cache-#{T(java.util.UUID).randomUUID().toString()}")
    public void onMessage(ConsumerRecord<String, String> record) throws JsonProcessingException {
        handler.handle(objectMapper.readValue(record.value(), TokenEvent.class));
    }
}
