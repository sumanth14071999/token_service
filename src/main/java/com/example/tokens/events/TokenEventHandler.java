package com.example.tokens.events;

import com.example.tokens.events.TokenEvent.TokenBlocked;
import com.example.tokens.events.TokenEvent.TokenIssued;
import com.example.tokens.events.TokenEvent.TokenUnblocked;
import com.example.tokens.token.Token;
import com.example.tokens.token.TokenCache;
import com.example.tokens.token.TokenRepository;
import com.example.tokens.token.TokenStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TokenEventHandler {

    private static final Logger log = LoggerFactory.getLogger(TokenEventHandler.class);

    private final TokenRepository tokens;
    private final TokenCache cache;

    public TokenEventHandler(TokenRepository tokens, TokenCache cache) {
        this.tokens = tokens;
        this.cache = cache;
    }

    @Transactional
    public void handle(TokenEvent event) {
        switch (event) {
            case TokenIssued e -> tokens.save(new Token(e.uid(), e.contractId(), TokenStatus.ACTIVE, e.validUntil()));
            case TokenBlocked e -> find(e.uid()).block();
            case TokenUnblocked e -> find(e.uid()).unblock();
        }
        cache.evict(event.uid());
        log.info("Applied {} for token {}", event.getClass().getSimpleName(), event.uid());
    }

    private Token find(String uid) {
        return tokens.findById(uid).orElseThrow(() -> new IllegalStateException("Unknown token " + uid));
    }
}
