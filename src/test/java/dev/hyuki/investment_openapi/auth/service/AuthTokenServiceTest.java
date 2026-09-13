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
import dev.hyuki.investment_openapi.auth.token.JwtTokenProvider;
import dev.hyuki.investment_openapi.auth.token.TokenPair;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
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
        refreshTokenHasher,
        Clock.fixed(NOW, ZoneOffset.UTC)
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
        .isInstanceOf(RedisConnectionFailureException.class);
  }

  private TokenPair pair(UUID sessionId) {
    return new TokenPair(
        "access-token",
        NOW.plusSeconds(1800),
        "refresh-token",
        NOW.plusSeconds(86400),
        sessionId
    );
  }
}
