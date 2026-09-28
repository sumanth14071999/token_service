package com.example.tokens.token;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** In-memory cache of tokens, so authorizations don't hit the database every time. */
@Component
public class TokenCache {

    private record Entry(Token token, Instant expiresAt) {
    }

    private final Map<String, Entry> entries = new ConcurrentHashMap<>();
    private final Clock clock;
    private final Duration ttl;

    public TokenCache(Clock clock, @Value("${app.token-cache.ttl}") Duration ttl) {
        this.clock = clock;
        this.ttl = ttl;
    }

    public Optional<Token> get(String uid) {
        Entry entry = entries.get(uid);
        if (entry == null) {
            return Optional.empty();
        }
        if (Instant.now(clock).isBefore(entry.expiresAt())) {
            entries.remove(uid);
            return Optional.empty();
        }
        return Optional.of(entry.token());
    }

    public void put(Token token) {
        entries.put(token.getUid(), new Entry(token, Instant.now(clock).plus(ttl)));
    }

    public void evict(String uid) {
        entries.remove(uid);
    }
}
