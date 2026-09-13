package dev.hyuki.investment_openapi.auth.service;

import dev.hyuki.investment_openapi.auth.session.AuthSession;
import dev.hyuki.investment_openapi.auth.session.RedisSessionStore;
import dev.hyuki.investment_openapi.auth.session.RefreshTokenHasher;
import dev.hyuki.investment_openapi.auth.token.JwtTokenProvider;
import dev.hyuki.investment_openapi.auth.token.TokenPair;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class AuthTokenService {

  private final JwtTokenProvider tokenProvider;
  private final RedisSessionStore sessionStore;
  private final RefreshTokenHasher refreshTokenHasher;
  private final Clock clock;

  public AuthTokenService(
      JwtTokenProvider tokenProvider,
      RedisSessionStore sessionStore,
      RefreshTokenHasher refreshTokenHasher,
      Clock clock
  ) {
    this.tokenProvider = tokenProvider;
    this.sessionStore = sessionStore;
    this.refreshTokenHasher = refreshTokenHasher;
    this.clock = clock;
  }

  public TokenPair issueSession(UUID userId) {
    Objects.requireNonNull(userId, "userId must not be null");
    UUID sessionId = UUID.randomUUID();
    TokenPair pair = tokenProvider.issue(userId, sessionId);
    Instant issuedAt = clock.instant();
    sessionStore.save(new AuthSession(
        userId,
        sessionId,
        refreshTokenHasher.hash(pair.refreshToken()),
        issuedAt,
        pair.refreshExpiresAt()
    ));
    return pair;
  }
}
