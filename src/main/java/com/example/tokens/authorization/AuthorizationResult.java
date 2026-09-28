package com.example.tokens.authorization;

/** Subset of OCPI 2.2.1 AllowedType, plus UNKNOWN for tokens we have never issued. */
public enum AuthorizationResult {
    ALLOWED,
    BLOCKED,
    EXPIRED,
    UNKNOWN
}
