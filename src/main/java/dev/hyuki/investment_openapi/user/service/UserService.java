package dev.hyuki.investment_openapi.user.service;

import dev.hyuki.investment_openapi.auth.config.LoginProperties;
import dev.hyuki.investment_openapi.auth.service.AuthTokenService;
import dev.hyuki.investment_openapi.auth.token.TokenPair;
import dev.hyuki.investment_openapi.support.error.ApiException;
import dev.hyuki.investment_openapi.support.error.ErrorCode;
import dev.hyuki.investment_openapi.user.entity.User;
import dev.hyuki.investment_openapi.user.repository.UserRepository;
import dev.hyuki.investment_openapi.user.support.EmailNormalizer;
import java.time.Clock;
import java.time.Instant;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final AuthTokenService authTokenService;
  private final LoginProperties loginProperties;
  private final Clock clock;

  public UserService(
      UserRepository userRepository,
      PasswordEncoder passwordEncoder,
      AuthTokenService authTokenService,
      LoginProperties loginProperties,
      Clock clock
  ) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
    this.authTokenService = authTokenService;
    this.loginProperties = loginProperties;
    this.clock = clock;
  }

  @Transactional
  public RegisteredUserSession register(String email, String rawPassword) {
    String normalizedEmail = EmailNormalizer.normalize(email);
    if (userRepository.existsByEmail(normalizedEmail)) {
      throw emailAlreadyExists();
    }

    User user = User.register(
        normalizedEmail,
        passwordEncoder.encode(rawPassword),
        clock.instant()
    );

    RegisteredUser registeredUser;
    try {
      registeredUser = RegisteredUser.from(userRepository.saveAndFlush(user));
    } catch (DataIntegrityViolationException exception) {
      throw emailAlreadyExists(exception);
    }
    TokenPair tokens = authTokenService.issueSession(registeredUser.userId());
    return new RegisteredUserSession(registeredUser, tokens);
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void recordLoginFailure(String email, Instant failedAt) {
    userRepository.recordLoginFailure(
        EmailNormalizer.normalize(email),
        loginProperties.maxFailedAttempts(),
        failedAt.plus(loginProperties.lockDuration()),
        failedAt
    );
  }

  private ApiException emailAlreadyExists() {
    return new ApiException(
        ErrorCode.EMAIL_ALREADY_EXISTS,
        "The normalized email is already registered."
    );
  }

  private ApiException emailAlreadyExists(DataIntegrityViolationException cause) {
    return new ApiException(
        ErrorCode.EMAIL_ALREADY_EXISTS,
        "The normalized email is already registered.",
        cause
    );
  }
}
