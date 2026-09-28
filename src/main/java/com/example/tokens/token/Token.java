package com.example.tokens.token;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** An RFID charging card. {@code uid} is the card's RFID UID as reported by the charger. */
@Entity
@Table(name = "token")
public class Token {

    @Id
    private String uid;

    @Column(name = "contract_id", nullable = false)
    private String contractId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TokenStatus status;

    /** Date printed on the card; {@code null} means the card does not expire. See README "Expiry". */
    @Column(name = "valid_until")
    private LocalDate validUntil;

    protected Token() {
        // for JPA
    }

    public Token(String uid, String contractId, TokenStatus status, LocalDate validUntil) {
        this.uid = uid;
        this.contractId = contractId;
        this.status = status;
        this.validUntil = validUntil;
    }

    public boolean isExpiredAt(Instant now) {
        return validUntil != null && now.isAfter(validUntil.atStartOfDay(ZoneOffset.UTC).toInstant());
    }

    public boolean isBlocked() {
        return status == TokenStatus.BLOCKED;
    }

    public void block() {
        this.status = TokenStatus.BLOCKED;
    }

    public void unblock() {
        this.status = TokenStatus.ACTIVE;
    }

    public String getUid() {
        return uid;
    }

    public String getContractId() {
        return contractId;
    }

    public TokenStatus getStatus() {
        return status;
    }

    public LocalDate getValidUntil() {
        return validUntil;
    }
}
