package dev.hyuki.investment_openapi.auth.token;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import dev.hyuki.investment_openapi.auth.config.JwtProperties;
import dev.hyuki.investment_openapi.support.error.ApiException;
import dev.hyuki.investment_openapi.support.error.ErrorCode;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenProvider {

  private static final String TOKEN_USE_CLAIM = "token_use";
  private static final String SESSION_ID_CLAIM = "sid";
  private static final int MAX_TOKEN_LENGTH = 4096;

  private final JwtProperties properties;
  private final Clock clock;
  private final NimbusJwtEncoder encoder;
  private final NimbusJwtDecoder decoder;

  public JwtTokenProvider(JwtProperties properties, Clock clock) {
    this.properties = properties;
    this.clock = clock;
    SecretKey secretKey = secretKey(properties.secret());
    this.encoder = new NimbusJwtEncoder(new ImmutableSecret<>(secretKey));
    this.decoder = NimbusJwtDecoder.withSecretKey(secretKey)
        .macAlgorithm(MacAlgorithm.HS256)
        .build();
    this.decoder.setJwtValidator(token -> OAuth2TokenValidatorResult.success());
  }

  public TokenPair issue(UUID userId, UUID sessionId) {
    Objects.requireNonNull(userId, "userId must not be null");
    Objects.requireNonNull(sessionId, "sessionId must not be null");
    Instant issuedAt = clock.instant();
    Instant accessExpiresAt = issuedAt.plus(properties.accessTokenTtl());
    Instant refreshExpiresAt = issuedAt.plus(properties.refreshTokenTtl());
    return new TokenPair(
        encode(userId, sessionId, issuedAt, accessExpiresAt, "access"),
        accessExpiresAt,
        encode(userId, sessionId, issuedAt, refreshExpiresAt, "refresh"),
        refreshExpiresAt,
        sessionId,
        issuedAt
    );
  }

  public TokenClaims readAccess(String token) {
    return read(token, "access");
  }

  public TokenClaims readRefresh(String token) {
    return read(token, "refresh");
  }

  private String encode(
      UUID userId,
      UUID sessionId,
      Instant issuedAt,
      Instant expiresAt,
      String tokenUse
  ) {
    JwtClaimsSet claims = JwtClaimsSet.builder()
        .issuer(properties.issuer())
        .audience(List.of(properties.audience()))
        .subject(userId.toString())
        .id(UUID.randomUUID().toString())
        .issuedAt(issuedAt)
        .notBefore(issuedAt)
        .expiresAt(expiresAt)
        .claim(SESSION_ID_CLAIM, sessionId.toString())
        .claim(TOKEN_USE_CLAIM, tokenUse)
        .build();
    JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).type("JWT").build();
    return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
  }

  private TokenClaims read(String token, String expectedTokenUse) {
    if (token == null || token.isBlank() || token.length() > MAX_TOKEN_LENGTH) {
      throw invalidToken();
    }

    Jwt jwt;
    try {
      jwt = decoder.decode(token);
    } catch (JwtException | IllegalArgumentException exception) {
      throw invalidToken(exception);
    }
    return validateClaims(jwt, expectedTokenUse);
  }

  private TokenClaims validateClaims(Jwt jwt, String expectedTokenUse) {
    Instant now = clock.instant();
    Instant issuedAt = jwt.getIssuedAt();
    Instant notBefore = jwt.getNotBefore();
    Instant expiresAt = jwt.getExpiresAt();

    if (expiresAt != null && !expiresAt.isAfter(now)) {
      throw new ApiException(ErrorCode.TOKEN_EXPIRED, "The token has expired.");
    }
    if (issuedAt == null || notBefore == null || expiresAt == null
        || issuedAt.isAfter(now) || notBefore.isAfter(now) || !expiresAt.isAfter(issuedAt)
        || !properties.issuer().equals(jwt.getClaimAsString("iss"))
        || jwt.getAudience() == null || !jwt.getAudience().contains(properties.audience())
        || jwt.getId() == null || jwt.getId().isBlank()
        || !expectedTokenUse.equals(jwt.getClaimAsString(TOKEN_USE_CLAIM))) {
      throw invalidToken();
    }

    try {
      UUID userId = UUID.fromString(jwt.getSubject());
      UUID sessionId = UUID.fromString(jwt.getClaimAsString(SESSION_ID_CLAIM));
      return new TokenClaims(userId, sessionId, jwt.getId(), issuedAt, expiresAt);
    } catch (IllegalArgumentException | NullPointerException exception) {
      throw invalidToken(exception);
    }
  }

  private SecretKey secretKey(String encodedSecret) {
    byte[] keyBytes;
    try {
      keyBytes = Base64.getDecoder().decode(encodedSecret);
    } catch (IllegalArgumentException exception) {
      throw new IllegalArgumentException("security.jwt.secret must be valid Base64", exception);
    }
    if (keyBytes.length < 32) {
      throw new IllegalArgumentException("security.jwt.secret must contain at least 32 bytes");
    }
    return new SecretKeySpec(keyBytes, "HmacSHA256");
  }

  private ApiException invalidToken() {
    return new ApiException(ErrorCode.INVALID_TOKEN, "The token is invalid.");
  }

  private ApiException invalidToken(Throwable cause) {
    return new ApiException(ErrorCode.INVALID_TOKEN, "The token is invalid.", cause);
  }
}
