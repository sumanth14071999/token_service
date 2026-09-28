package com.example.tokens.authorization;

/**
 * @param authorizationReference only set when ALLOWED; the CPO echoes it on the session and CDR
 */
public record AuthorizationDecision(
        String tokenUid,
        AuthorizationResult result,
        String contractId,
        String authorizationReference) {
}
