package com.example.tokens.token;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;

import com.example.tokens.support.MutableClock;
import org.junit.jupiter.api.Test;

class TokenCacheTest {

    private final MutableClock clock = new MutableClock(Instant.parse("2026-01-10T10:00:00Z"));
    private final TokenCache cache = new TokenCache(clock, Duration.ofMinutes(10));
    private final Token token = new Token("04A2B3C4D5E6F7", "DE-DCS-C12345678-X", TokenStatus.ACTIVE,
            LocalDate.parse("2027-12-31"));

    @Test
    void returnsCachedTokenWithinTtl() {
        cache.put(token);
        clock.advance(Duration.ofMinutes(9));

        assertThat(cache.get(token.getUid())).contains(token);
    }

    @Test
    void forgetsTokenAfterTtl() {
        cache.put(token);
        clock.advance(Duration.ofMinutes(11));

        assertThat(cache.get(token.getUid())).isEmpty();
    }

    @Test
    void evictedTokenIsGone() {
        cache.put(token);
        cache.evict(token.getUid());

        assertThat(cache.get(token.getUid())).isEmpty();
    }
}
