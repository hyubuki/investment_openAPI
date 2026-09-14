package dev.hyuki.investment_openapi.auth.service;

import dev.hyuki.investment_openapi.auth.session.AuthSession;
import dev.hyuki.investment_openapi.auth.session.RedisSessionStore;
import dev.hyuki.investment_openapi.auth.session.RefreshTokenHasher;
import dev.hyuki.investment_openapi.auth.session.SessionRotationResult;
import dev.hyuki.investment_openapi.auth.token.JwtTokenProvider;
import dev.hyuki.investment_openapi.auth.token.TokenClaims;
import dev.hyuki.investment_openapi.auth.token.TokenPair;
import dev.hyuki.investment_openapi.support.error.ApiException;
import dev.hyuki.investment_openapi.support.error.ErrorCode;
import java.util.Objects;
import java.util.UUID;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

@Service
public class AuthTokenService {

  private final JwtTokenProvider tokenProvider;
  private final RedisSessionStore sessionStore;
  private final RefreshTokenHasher refreshTokenHasher;

  public AuthTokenService(
      JwtTokenProvider tokenProvider,
      RedisSessionStore sessionStore,
      RefreshTokenHasher refreshTokenHasher
  ) {
    this.tokenProvider = tokenProvider;
    this.sessionStore = sessionStore;
    this.refreshTokenHasher = refreshTokenHasher;
  }

  public TokenPair issueSession(UUID userId) {
    Objects.requireNonNull(userId, "userId must not be null");
    UUID sessionId = UUID.randomUUID();
    TokenPair pair = tokenProvider.issue(userId, sessionId);
    try {
      sessionStore.save(session(pair, userId));
    } catch (DataAccessException | IllegalStateException exception) {
      throw authenticationUnavailable(
          "The authentication session could not be created.",
          exception
      );
    }
    return pair;
  }

  public TokenPair rotateSession(String refreshToken) {
    TokenClaims claims = tokenProvider.readRefresh(refreshToken);
    TokenPair replacement = tokenProvider.issue(claims.userId(), claims.sessionId());
    SessionRotationResult result;
    try {
      result = sessionStore.rotate(
          session(replacement, claims.userId()),
          refreshTokenHasher.hash(refreshToken)
      );
    } catch (DataAccessException | IllegalStateException exception) {
      throw authenticationUnavailable(
          "The authentication session could not be refreshed.",
          exception
      );
    }
    return switch (result) {
      case ROTATED -> replacement;
      case REUSED -> throw new ApiException(
          ErrorCode.REFRESH_TOKEN_REUSED,
          "The refresh token has already been rotated."
      );
      case INVALID -> throw new ApiException(
          ErrorCode.SESSION_INVALID,
          "The authentication session is invalid."
      );
    };
  }

  private AuthSession session(TokenPair pair, UUID userId) {
    return new AuthSession(
        userId,
        pair.sessionId(),
        refreshTokenHasher.hash(pair.refreshToken()),
        pair.issuedAt(),
        pair.refreshExpiresAt()
    );
  }

  private ApiException authenticationUnavailable(String message, RuntimeException cause) {
    return new ApiException(ErrorCode.AUTHENTICATION_UNAVAILABLE, message, cause);
  }
}
