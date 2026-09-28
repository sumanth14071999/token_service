package com.example.tokens.authorization;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import com.example.tokens.audit.AuthorizationAuditLogger;
import com.example.tokens.token.Token;
import com.example.tokens.token.TokenCache;
import com.example.tokens.token.TokenRepository;
import org.springframework.stereotype.Service;

@Service
public class TokenAuthorizationService {

    private final TokenRepository tokens;
    private final TokenCache cache;
    private final Clock clock;
    private final AuthorizationAuditLogger auditLogger;

    public TokenAuthorizationService(TokenRepository tokens, TokenCache cache, Clock clock,
            AuthorizationAuditLogger auditLogger) {
        this.tokens = tokens;
        this.cache = cache;
        this.clock = clock;
        this.auditLogger = auditLogger;
    }

    /** May this token charge at this location right now? See README "Authorization rules". */
    public AuthorizationDecision authorize(String tokenUid, String locationId) {
        AuthorizationDecision decision = decide(tokenUid);
        auditLogger.log(tokenUid, locationId, decision);
        return decision;
    }

    private AuthorizationDecision decide(String tokenUid) {
        Optional<Token> found = cache.get(tokenUid).or(() -> {
            Optional<Token> fromDb = tokens.findById(tokenUid);
            fromDb.ifPresent(cache::put);
            return fromDb;
        });
        if (found.isEmpty()) {
            return new AuthorizationDecision(tokenUid, AuthorizationResult.UNKNOWN, null, null);
        }

        Token token = found.get();
        if (token.isBlocked()) {
            return new AuthorizationDecision(tokenUid, AuthorizationResult.BLOCKED, token.getContractId(), null);
        }
        if (token.isExpiredAt(Instant.now(clock))) {
            return new AuthorizationDecision(tokenUid, AuthorizationResult.EXPIRED, token.getContractId(), null);
        }
        return new AuthorizationDecision(tokenUid, AuthorizationResult.ALLOWED, token.getContractId(),
                UUID.randomUUID().toString());
    }
}
