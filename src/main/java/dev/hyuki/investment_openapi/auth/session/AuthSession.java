package dev.hyuki.investment_openapi.auth.session;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record AuthSession(
    UUID userId,
    UUID sessionId,
    String refreshTokenHash,
    Instant issuedAt,
    Instant expiresAt
) {

  public AuthSession {
    Objects.requireNonNull(userId, "userId must not be null");
    Objects.requireNonNull(sessionId, "sessionId must not be null");
    Objects.requireNonNull(refreshTokenHash, "refreshTokenHash must not be null");
    Objects.requireNonNull(issuedAt, "issuedAt must not be null");
    Objects.requireNonNull(expiresAt, "expiresAt must not be null");
    if (!expiresAt.isAfter(issuedAt)) {
      throw new IllegalArgumentException("expiresAt must be after issuedAt");
    }
  }

  @Override
  public String toString() {
    return "AuthSession[userId=" + userId + ", sessionId=" + sessionId
        + ", refreshTokenHash=[REDACTED], issuedAt=" + issuedAt
        + ", expiresAt=" + expiresAt + "]";
  }
}
