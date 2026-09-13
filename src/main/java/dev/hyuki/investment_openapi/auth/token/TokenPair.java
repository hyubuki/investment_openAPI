package dev.hyuki.investment_openapi.auth.token;

import java.time.Instant;
import java.util.UUID;

public record TokenPair(
    String accessToken,
    Instant accessExpiresAt,
    String refreshToken,
    Instant refreshExpiresAt,
    UUID sessionId
) {

  @Override
  public String toString() {
    return "TokenPair[accessToken=[REDACTED], accessExpiresAt=" + accessExpiresAt
        + ", refreshToken=[REDACTED], refreshExpiresAt=" + refreshExpiresAt
        + ", sessionId=" + sessionId + "]";
  }
}
