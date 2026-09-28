package com.example.tokens.events;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/** Token lifecycle events from customer-account-service on {@code tokens.events}; record key = {@code uid}. */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = TokenEvent.TokenIssued.class, name = "TOKEN_ISSUED"),
        @JsonSubTypes.Type(value = TokenEvent.TokenBlocked.class, name = "TOKEN_BLOCKED"),
        @JsonSubTypes.Type(value = TokenEvent.TokenUnblocked.class, name = "TOKEN_UNBLOCKED")
})
public sealed interface TokenEvent
        permits TokenEvent.TokenIssued, TokenEvent.TokenBlocked, TokenEvent.TokenUnblocked {

    String uid();

    record TokenIssued(String uid, String contractId, LocalDate validUntil) implements TokenEvent {
    }

    record TokenBlocked(String uid, String reason) implements TokenEvent {
    }

    record TokenUnblocked(String uid) implements TokenEvent {
    }
}
