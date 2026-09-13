package dev.hyuki.investment_openapi.auth.token;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import dev.hyuki.investment_openapi.auth.config.JwtProperties;
import dev.hyuki.investment_openapi.support.error.ApiException;
import dev.hyuki.investment_openapi.support.error.ErrorCode;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

class JwtTokenProviderTest {

  private static final String KEY =
      "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";
  private static final Instant NOW = Instant.parse("2026-09-13T01:00:00Z");

  private Clock clock;
  private UUID userId;
  private UUID sessionId;
  private JwtTokenProvider tokenProvider;

  @BeforeEach
  void setUp() {
    clock = mock(Clock.class);
    when(clock.instant()).thenReturn(NOW);
    userId = UUID.randomUUID();
    sessionId = UUID.randomUUID();
    tokenProvider = provider("test-issuer", "test-audience", KEY, clock);
  }

  @Test
  @DisplayName("Access와 Refresh Token에 사용자·세션 식별자와 서로 다른 만료 시간을 담아 발급한다")
  void issuesTokenPairWithRequiredClaimsAndLifetimes() {
    TokenPair pair = tokenProvider.issue(userId, sessionId);

    assertThat(tokenProvider.readAccess(pair.accessToken()))
        .returns(userId, TokenClaims::userId)
        .returns(sessionId, TokenClaims::sessionId)
        .returns(NOW.plusSeconds(1800), TokenClaims::expiresAt);
    assertThat(tokenProvider.readRefresh(pair.refreshToken()))
        .returns(userId, TokenClaims::userId)
        .returns(sessionId, TokenClaims::sessionId)
        .returns(NOW.plusSeconds(86400), TokenClaims::expiresAt);
    assertThat(pair.accessToken()).isNotEqualTo(pair.refreshToken());
    assertThat(tokenProvider.issue(userId, sessionId).refreshToken())
        .isNotEqualTo(pair.refreshToken());
    assertThat(pair.toString()).doesNotContain(pair.accessToken(), pair.refreshToken());
  }

  @Test
  @DisplayName("Token 만료 시각부터 TOKEN_EXPIRED 오류로 거부한다")
  void rejectsTokensAtExactExpiry() {
    TokenPair pair = tokenProvider.issue(userId, sessionId);

    when(clock.instant()).thenReturn(NOW.plusSeconds(1800));
    expectError(() -> tokenProvider.readAccess(pair.accessToken()), ErrorCode.TOKEN_EXPIRED);
    assertThat(tokenProvider.readRefresh(pair.refreshToken()).userId()).isEqualTo(userId);

    when(clock.instant()).thenReturn(NOW.plusSeconds(86400));
    expectError(() -> tokenProvider.readRefresh(pair.refreshToken()), ErrorCode.TOKEN_EXPIRED);
  }

  @Test
  @DisplayName("Access Token과 Refresh Token의 용도를 바꾸어 사용할 수 없다")
  void rejectsInterchangedTokenUse() {
    TokenPair pair = tokenProvider.issue(userId, sessionId);

    expectError(() -> tokenProvider.readAccess(pair.refreshToken()), ErrorCode.INVALID_TOKEN);
    expectError(() -> tokenProvider.readRefresh(pair.accessToken()), ErrorCode.INVALID_TOKEN);
  }

  @ParameterizedTest(name = "신뢰 경계 변경: {0}")
  @ValueSource(strings = {"key", "issuer", "audience", "future"})
  @DisplayName("서명 키·issuer·audience·발급 시각이 신뢰 경계를 벗어난 Token을 거부한다")
  void rejectsTokensOutsideTrustBoundary(String changedValue) {
    String key = changedValue.equals("key")
        ? Base64.getEncoder().encodeToString(new byte[32])
        : KEY;
    String issuer = changedValue.equals("issuer") ? "other-issuer" : "test-issuer";
    String audience = changedValue.equals("audience") ? "other-audience" : "test-audience";
    Clock issuerClock = changedValue.equals("future")
        ? Clock.fixed(NOW.plusSeconds(60), java.time.ZoneOffset.UTC)
        : Clock.fixed(NOW, java.time.ZoneOffset.UTC);
    JwtTokenProvider untrustedProvider = provider(issuer, audience, key, issuerClock);
    String token = untrustedProvider.issue(userId, sessionId).accessToken();

    expectError(() -> tokenProvider.readAccess(token), ErrorCode.INVALID_TOKEN);
  }

  @ParameterizedTest(name = "누락 Claim: {0}")
  @ValueSource(strings = {"iss", "aud", "exp", "iat", "nbf", "sub", "sid", "jti", "token_use"})
  @DisplayName("필수 Claim이 하나라도 없는 서명된 Token을 거부한다")
  void rejectsSignedTokensMissingMandatoryClaims(String missingClaim) {
    JwtClaimsSet claims = JwtClaimsSet.builder()
        .issuer("test-issuer")
        .audience(List.of("test-audience"))
        .subject(userId.toString())
        .id(UUID.randomUUID().toString())
        .issuedAt(NOW)
        .notBefore(NOW)
        .expiresAt(NOW.plusSeconds(1800))
        .claim("sid", sessionId.toString())
        .claim("token_use", "access")
        .claims(values -> values.remove(missingClaim))
        .build();
    NimbusJwtEncoder encoder = new NimbusJwtEncoder(new ImmutableSecret<>(
        new SecretKeySpec(Base64.getDecoder().decode(KEY), "HmacSHA256")
    ));
    String token = encoder.encode(JwtEncoderParameters.from(
        JwsHeader.with(MacAlgorithm.HS256).build(), claims
    )).getTokenValue();

    expectError(() -> tokenProvider.readAccess(token), ErrorCode.INVALID_TOKEN);
  }

  @Test
  @DisplayName("형식이 잘못되거나 4096자를 초과한 Token을 거부한다")
  void rejectsMalformedAndOversizedTokens() {
    expectError(() -> tokenProvider.readAccess("not-a-token"), ErrorCode.INVALID_TOKEN);
    expectError(() -> tokenProvider.readRefresh("x".repeat(4097)), ErrorCode.INVALID_TOKEN);
  }

  @Test
  @DisplayName("Base64 형식이 아니거나 256bit보다 짧은 HMAC Key 설정을 거부한다")
  void rejectsInvalidSecretConfiguration() {
    assertThatThrownBy(() -> provider("issuer", "audience", "invalid!", clock))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Base64");
    assertThatThrownBy(() -> provider("issuer", "audience", "YQ==", clock))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("32 bytes");
  }

  private JwtTokenProvider provider(String issuer, String audience, String key, Clock tokenClock) {
    return new JwtTokenProvider(
        new JwtProperties(
            issuer,
            audience,
            key,
            Duration.ofMinutes(30),
            Duration.ofHours(24)
        ),
        tokenClock
    );
  }

  private void expectError(Runnable action, ErrorCode expectedCode) {
    assertThatThrownBy(action::run)
        .isInstanceOfSatisfying(
            ApiException.class,
            exception -> assertThat(exception.code()).isEqualTo(expectedCode)
        );
  }
}
