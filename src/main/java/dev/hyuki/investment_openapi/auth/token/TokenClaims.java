package dev.hyuki.investment_openapi.auth.token;

import java.time.Instant;
import java.util.UUID;

public record TokenClaims(
    UUID userId,
    UUID sessionId,
    String tokenId,
    Instant issuedAt,
    Instant expiresAt
) {
}
