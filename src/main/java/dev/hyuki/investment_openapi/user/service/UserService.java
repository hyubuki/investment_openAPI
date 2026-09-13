package dev.hyuki.investment_openapi.user.service;

import dev.hyuki.investment_openapi.user.entity.User;
import dev.hyuki.investment_openapi.user.repository.UserRepository;
import dev.hyuki.investment_openapi.user.support.EmailNormalizer;
import dev.hyuki.investment_openapi.support.error.ApiException;
import dev.hyuki.investment_openapi.support.error.ErrorCode;
import java.time.Clock;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final Clock clock;

  public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, Clock clock) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
    this.clock = clock;
  }

  @Transactional
  public RegisteredUser register(String email, String rawPassword) {
    String normalizedEmail = EmailNormalizer.normalize(email);
    if (userRepository.existsByEmail(normalizedEmail)) {
      throw emailAlreadyExists();
    }

    User user = User.register(
        normalizedEmail,
        passwordEncoder.encode(rawPassword),
        clock.instant()
    );

    try {
      return RegisteredUser.from(userRepository.saveAndFlush(user));
    } catch (DataIntegrityViolationException exception) {
      throw emailAlreadyExists(exception);
    }
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
