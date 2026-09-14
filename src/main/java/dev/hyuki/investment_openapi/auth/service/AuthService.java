package dev.hyuki.investment_openapi.auth.service;

import dev.hyuki.investment_openapi.auth.session.RedisSessionStore;
import dev.hyuki.investment_openapi.auth.token.JwtTokenProvider;
import dev.hyuki.investment_openapi.auth.token.TokenClaims;
import dev.hyuki.investment_openapi.auth.token.TokenPair;
import dev.hyuki.investment_openapi.support.error.ApiException;
import dev.hyuki.investment_openapi.support.error.ErrorCode;
import dev.hyuki.investment_openapi.user.entity.User;
import dev.hyuki.investment_openapi.user.entity.UserStatus;
import dev.hyuki.investment_openapi.user.repository.UserRepository;
import dev.hyuki.investment_openapi.user.support.EmailNormalizer;
import java.time.Clock;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DataAccessException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final AuthTokenService authTokenService;
  private final JwtTokenProvider tokenProvider;
  private final RedisSessionStore sessionStore;
  private final Clock clock;
  private final String dummyPasswordHash;

  public AuthService(
      UserRepository userRepository,
      PasswordEncoder passwordEncoder,
      AuthTokenService authTokenService,
      JwtTokenProvider tokenProvider,
      RedisSessionStore sessionStore,
      Clock clock
  ) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
    this.authTokenService = authTokenService;
    this.tokenProvider = tokenProvider;
    this.sessionStore = sessionStore;
    this.clock = clock;
    this.dummyPasswordHash = passwordEncoder.encode(UUID.randomUUID().toString());
  }

  @Transactional
  public TokenPair login(String email, String rawPassword) {
    Optional<User> candidate = userRepository.findByEmail(EmailNormalizer.normalize(email));
    String passwordHash = candidate.map(User::getPasswordHash).orElse(dummyPasswordHash);
    boolean passwordMatches = passwordEncoder.matches(rawPassword, passwordHash);
    if (candidate.isEmpty() || !passwordMatches) {
      throw new ApiException(
          ErrorCode.INVALID_CREDENTIALS,
          "The supplied credentials are invalid."
      );
    }

    User user = candidate.orElseThrow();
    requireActive(user);
    TokenPair pair = authTokenService.issueSession(user.getUserId());
    user.recordSuccessfulLogin(clock.instant());
    return pair;
  }

  public TokenPair refresh(String refreshToken) {
    TokenClaims claims = tokenProvider.readRefresh(refreshToken);
    User user = userRepository.findById(claims.userId())
        .orElseThrow(() -> new ApiException(
            ErrorCode.SESSION_INVALID,
            "The authentication session is invalid."
        ));
    requireActive(user);
    return authTokenService.rotateSession(refreshToken);
  }

  public void logout(String accessToken) {
    TokenClaims claims = tokenProvider.readAccess(accessToken);
    sessionStore.deleteIfSessionMatches(claims.userId(), claims.sessionId());
  }

  @Transactional(readOnly = true)
  public User authenticateAccessToken(String accessToken) {
    TokenClaims claims = tokenProvider.readAccess(accessToken);
    requireActiveSession(claims);
    return findActiveUser(claims);
  }

  private void requireActiveSession(TokenClaims claims) {
    try {
      if (!sessionStore.hasActiveSession(claims.userId(), claims.sessionId())) {
        throw new ApiException(
            ErrorCode.SESSION_INVALID,
            "The authentication session is invalid."
        );
      }
    } catch (DataAccessException | IllegalStateException exception) {
      throw new ApiException(
          ErrorCode.AUTHENTICATION_UNAVAILABLE,
          "The authentication session could not be verified.",
          exception
      );
    }
  }

  private User findActiveUser(TokenClaims claims) {
    try {
      return userRepository.findByUserIdAndStatus(claims.userId(), UserStatus.ACTIVE)
          .orElseThrow(() -> new ApiException(
              ErrorCode.USER_INACTIVE,
              "The authenticated user is not active."
          ));
    } catch (DataAccessException exception) {
      throw new ApiException(
          ErrorCode.AUTHENTICATION_UNAVAILABLE,
          "The authenticated user could not be verified.",
          exception
      );
    }
  }

  private void requireActive(User user) {
    if (user.getStatus() != UserStatus.ACTIVE) {
      throw new ApiException(ErrorCode.USER_INACTIVE, "The user is not active.");
    }
  }
}
