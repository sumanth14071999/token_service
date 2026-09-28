package com.example.tokens.authorization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

import com.example.tokens.audit.AuthorizationAuditLogger;
import com.example.tokens.audit.AuthorizationLogRepository;
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
class TokenAuthorizationServiceTest {

    private static final String UID = "04A2B3C4D5E6F7";
    private static final String LOCATION = "LOC-BER-HBF";

    @Mock
    private TokenRepository tokens;

    @Mock
    private AuthorizationLogRepository auditLogRepository;

    @Test
    void activeTokenIsAllowed() {
        givenToken(TokenStatus.ACTIVE, LocalDate.parse("2027-12-31"));

        AuthorizationDecision decision = serviceAt("2026-01-10T10:00:00Z").authorize(UID, LOCATION);

        assertThat(decision.result()).isEqualTo(AuthorizationResult.ALLOWED);
        assertThat(decision.authorizationReference()).isNotBlank();
    }

    @Test
    void blockedTokenIsBlocked() {
        givenToken(TokenStatus.BLOCKED, LocalDate.parse("2027-12-31"));

        assertThat(serviceAt("2026-01-10T10:00:00Z").authorize(UID, LOCATION).result())
                .isEqualTo(AuthorizationResult.BLOCKED);
    }

    @Test
    void unknownTokenIsUnknown() {
        when(tokens.findById(UID)).thenReturn(Optional.empty());

        assertThat(serviceAt("2026-01-10T10:00:00Z").authorize(UID, LOCATION).result())
                .isEqualTo(AuthorizationResult.UNKNOWN);
    }

    @Test
    void tokenIsValidOnTheDatePrintedOnTheCard() {
        givenToken(TokenStatus.ACTIVE, LocalDate.parse("2026-01-10"));

        // 15:00 in Berlin on the last valid day
        assertThat(serviceAt("2026-01-10T14:00:00Z").authorize(UID, LOCATION).result())
                .isEqualTo(AuthorizationResult.ALLOWED);
    }

    @Test
    void tokenExpiresAtMidnightBerlinTime() {
        givenToken(TokenStatus.ACTIVE, LocalDate.parse("2026-01-10"));

        // 00:30 in Berlin on the day after (still 2026-01-10 in UTC)
        assertThat(serviceAt("2026-01-10T23:30:00Z").authorize(UID, LOCATION).result())
                .isEqualTo(AuthorizationResult.EXPIRED);
    }

    @Test
    void repeatedAuthorizationsAreServedFromCache() {
        givenToken(TokenStatus.ACTIVE, LocalDate.parse("2027-12-31"));
        TokenAuthorizationService service = serviceAt("2026-01-10T10:00:00Z");

        service.authorize(UID, LOCATION);
        service.authorize(UID, LOCATION);

        org.mockito.Mockito.verify(tokens, org.mockito.Mockito.times(1)).findById(UID);
    }

    private void givenToken(TokenStatus status, LocalDate validUntil) {
        when(tokens.findById(UID)).thenReturn(Optional.of(new Token(UID, "DE-DCS-C12345678-X", status, validUntil)));
    }

    private TokenAuthorizationService serviceAt(String instant) {
        MutableClock clock = new MutableClock(Instant.parse(instant));
        return new TokenAuthorizationService(tokens, new TokenCache(clock, Duration.ofMinutes(10)), clock,
                new AuthorizationAuditLogger(auditLogRepository, clock));
    }
}
