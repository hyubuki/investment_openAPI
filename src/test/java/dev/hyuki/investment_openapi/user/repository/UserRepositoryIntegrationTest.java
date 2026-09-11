package dev.hyuki.investment_openapi.user.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.hyuki.investment_openapi.user.entity.User;
import dev.hyuki.investment_openapi.user.entity.UserStatus;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
class UserRepositoryIntegrationTest {

  @Autowired
  private UserRepository userRepository;

  @BeforeEach
  void clearUsers() {
    userRepository.deleteAll();
  }

  @Test
  void persistsUserWithGeneratedIdAndActiveStatus() {
    User user = userRepository.saveAndFlush(
        User.register("Trader@Example.com", "password-hash", Instant.now())
    );

    assertThat(user.getUserId()).isNotNull();
    assertThat(user.getEmail()).isEqualTo("trader@example.com");
    assertThat(userRepository.findByUserIdAndStatus(user.getUserId(), UserStatus.ACTIVE))
        .contains(user);
  }

  @Test
  void databaseUniqueConstraintRejectsDuplicateEmail() {
    String email = "constraint@example.com";
    userRepository.saveAndFlush(User.register(email, "first-hash", Instant.now()));

    assertThatThrownBy(() -> userRepository.saveAndFlush(
        User.register(email, "second-hash", Instant.now())
    )).isInstanceOf(DataIntegrityViolationException.class);
  }
}
