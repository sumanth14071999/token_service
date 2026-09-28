package com.example.tokens.events;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

import com.example.tokens.support.MutableClock;
import com.example.tokens.token.Token;
import com.example.tokens.token.TokenCache;
import com.example.tokens.token.TokenRepository;
import com.example.tokens.token.TokenStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TokenEventHandlerTest {

    @Mock
    private TokenRepository tokens;

    private final TokenCache cache = new TokenCache(new MutableClock(Instant.parse("2026-01-10T10:00:00Z")),
            Duration.ofMinutes(10));

    @Test
    void blockedEventBlocksTokenAndEvictsIt() {
        Token token = new Token("04A2B3C4D5E6F7", "DE-DCS-C12345678-X", TokenStatus.ACTIVE,
                LocalDate.parse("2027-12-31"));
        when(tokens.findById("04A2B3C4D5E6F7")).thenReturn(Optional.of(token));
        cache.put(token);

        new TokenEventHandler(tokens, cache).handle(new TokenEvent.TokenBlocked("04A2B3C4D5E6F7", "STOLEN"));

        assertThat(token.getStatus()).isEqualTo(TokenStatus.BLOCKED);
        assertThat(cache.get("04A2B3C4D5E6F7")).isEmpty();
    }
}
