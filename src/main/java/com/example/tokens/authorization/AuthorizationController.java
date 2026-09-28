package com.example.tokens.authorization;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Called by the OCPI adapter when a CPO asks for real-time authorization of a token. */
@RestController
@RequestMapping("/api/v1/tokens")
public class AuthorizationController {

    private final TokenAuthorizationService service;

    public AuthorizationController(TokenAuthorizationService service) {
        this.service = service;
    }

    @GetMapping("/{uid}/authorization")
    public ResponseEntity<AuthorizationDecision> authorize(@PathVariable String uid,
                                                           @RequestParam String locationId) {
        AuthorizationDecision decision = service.authorize(uid, locationId);
        if (decision.result() == AuthorizationResult.UNKNOWN) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(decision);
    }
}
