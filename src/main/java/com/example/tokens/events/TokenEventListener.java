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

    @KafkaListener(topics = "${app.topics.token-events}")
    public void onMessage(ConsumerRecord<String, String> record) throws JsonProcessingException {
        handler.handle(objectMapper.readValue(record.value(), TokenEvent.class));
    }
}
