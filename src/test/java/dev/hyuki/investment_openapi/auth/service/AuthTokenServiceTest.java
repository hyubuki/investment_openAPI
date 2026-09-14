package dev.hyuki.investment_openapi.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.hyuki.investment_openapi.auth.session.AuthSession;
import dev.hyuki.investment_openapi.auth.session.RedisSessionStore;
import dev.hyuki.investment_openapi.auth.session.RefreshTokenHasher;
import dev.hyuki.investment_openapi.auth.session.SessionRotationResult;
import dev.hyuki.investment_openapi.auth.token.JwtTokenProvider;
import dev.hyuki.investment_openapi.auth.token.TokenClaims;
import dev.hyuki.investment_openapi.auth.token.TokenPair;
import dev.hyuki.investment_openapi.support.error.ApiException;
import dev.hyuki.investment_openapi.support.error.ErrorCode;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.RedisConnectionFailureException;

class AuthTokenServiceTest {

  private static final Instant NOW = Instant.parse("2026-09-13T01:00:00Z");

  private JwtTokenProvider tokenProvider;
  private RedisSessionStore sessionStore;
  private RefreshTokenHasher refreshTokenHasher;
  private AuthTokenService authTokenService;

  @BeforeEach
  void setUp() {
    tokenProvider = mock(JwtTokenProvider.class);
    sessionStore = mock(RedisSessionStore.class);
    refreshTokenHasher = new RefreshTokenHasher();
    authTokenService = new AuthTokenService(
        tokenProvider,
        sessionStore,
        refreshTokenHasher
    );
  }

  @Test
  @DisplayName("Token Pair 발급 후 Refresh Token Hash를 포함한 Session을 Redis에 저장한다")
  void issuesTokenPairAfterSavingSession() {
    UUID userId = UUID.randomUUID();
    when(tokenProvider.issue(eq(userId), any(UUID.class)))
        .thenAnswer(invocation -> pair(invocation.getArgument(1)));

    TokenPair issued = authTokenService.issueSession(userId);

    ArgumentCaptor<AuthSession> sessionCaptor = ArgumentCaptor.forClass(AuthSession.class);
    verify(sessionStore).save(sessionCaptor.capture());
    AuthSession savedSession = sessionCaptor.getValue();
    assertThat(savedSession.userId()).isEqualTo(userId);
    assertThat(savedSession.sessionId()).isEqualTo(issued.sessionId());
    assertThat(savedSession.refreshTokenHash())
        .isEqualTo(refreshTokenHasher.hash(issued.refreshToken()))
        .isNotEqualTo(issued.refreshToken());
    assertThat(savedSession.toString()).doesNotContain(savedSession.refreshTokenHash());
  }

  @Test
  @DisplayName("Redis Session 저장에 실패하면 Token Pair 발급도 실패 처리한다")
  void failsClosedWhenRedisSessionCannotBeSaved() {
    UUID userId = UUID.randomUUID();
    when(tokenProvider.issue(eq(userId), any(UUID.class)))
        .thenAnswer(invocation -> pair(invocation.getArgument(1)));
    doThrow(new RedisConnectionFailureException("test outage"))
        .when(sessionStore)
        .save(any(AuthSession.class));

    assertThatThrownBy(() -> authTokenService.issueSession(userId))
        .isInstanceOfSatisfying(
            ApiException.class,
            exception -> {
              assertThat(exception.code()).isEqualTo(ErrorCode.AUTHENTICATION_UNAVAILABLE);
              assertThat(exception.getCause()).isInstanceOf(RedisConnectionFailureException.class);
            }
        );
  }

  @Test
  @DisplayName("현재 Refresh Token Hash가 일치하면 같은 Session ID로 Token Pair를 회전한다")
  void rotatesTokenPairWithinCurrentSession() {
    UUID userId = UUID.randomUUID();
    UUID sessionId = UUID.randomUUID();
    String refreshToken = "current-refresh-token";
    TokenClaims claims = new TokenClaims(
        userId,
        sessionId,
        "refresh-jti",
        NOW,
        NOW.plusSeconds(86400)
    );
    TokenPair replacement = pair(sessionId);
    when(tokenProvider.readRefresh(refreshToken)).thenReturn(claims);
    when(tokenProvider.issue(userId, sessionId)).thenReturn(replacement);
    when(sessionStore.rotate(
        any(AuthSession.class),
        eq(refreshTokenHasher.hash(refreshToken))
    )).thenReturn(SessionRotationResult.ROTATED);

    TokenPair rotated = authTokenService.rotateSession(refreshToken);

    assertThat(rotated).isSameAs(replacement);
  }

  @Test
  @DisplayName("이미 회전된 Refresh Token이면 REFRESH_TOKEN_REUSED 오류를 반환한다")
  void rejectsReusedRefreshToken() {
    String refreshToken = "reused-refresh-token";
    UUID userId = UUID.randomUUID();
    UUID sessionId = UUID.randomUUID();
    when(tokenProvider.readRefresh(refreshToken)).thenReturn(new TokenClaims(
        userId,
        sessionId,
        "refresh-jti",
        NOW,
        NOW.plusSeconds(86400)
    ));
    when(tokenProvider.issue(userId, sessionId)).thenReturn(pair(sessionId));
    when(sessionStore.rotate(any(AuthSession.class), any(String.class)))
        .thenReturn(SessionRotationResult.REUSED);

    assertThatThrownBy(() -> authTokenService.rotateSession(refreshToken))
        .isInstanceOfSatisfying(
            ApiException.class,
            exception -> assertThat(exception.code()).isEqualTo(
                ErrorCode.REFRESH_TOKEN_REUSED
            )
        );
  }

  private TokenPair pair(UUID sessionId) {
    return new TokenPair(
        "access-token",
        NOW.plusSeconds(1800),
        "refresh-token",
        NOW.plusSeconds(86400),
        sessionId,
        NOW
    );
  }
}
