package dev.hyuki.investment_openapi.auth.dto;

import dev.hyuki.investment_openapi.auth.token.TokenPair;
import java.time.Duration;

public record AuthTokensResponse(
    String tokenType,
    String accessToken,
    long expiresIn,
    String refreshToken,
    long refreshExpiresIn
) {

  private static final String BEARER = "Bearer";

  public static AuthTokensResponse from(TokenPair pair) {
    return new AuthTokensResponse(
        BEARER,
        pair.accessToken(),
        Duration.between(pair.issuedAt(), pair.accessExpiresAt()).toSeconds(),
        pair.refreshToken(),
        Duration.between(pair.issuedAt(), pair.refreshExpiresAt()).toSeconds()
    );
  }

  @Override
  public String toString() {
    return "AuthTokensResponse[tokenType=" + tokenType
        + ", accessToken=[REDACTED], expiresIn=" + expiresIn
        + ", refreshToken=[REDACTED], refreshExpiresIn=" + refreshExpiresIn + "]";
  }
}
