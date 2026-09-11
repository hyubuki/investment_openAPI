package dev.hyuki.investment_openapi.user.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.hyuki.investment_openapi.user.entity.User;
import dev.hyuki.investment_openapi.user.entity.UserStatus;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
  @DisplayName("사용자를 저장하면 UUID가 생성되고 정규화된 이메일과 ACTIVE 상태로 조회된다")
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
  @DisplayName("정규화된 이메일이 중복되면 데이터베이스 유일성 제약조건이 저장을 거부한다")
  void databaseUniqueConstraintRejectsDuplicateEmail() {
    String email = "constraint@example.com";
    userRepository.saveAndFlush(User.register(email, "first-hash", Instant.now()));

    assertThatThrownBy(() -> userRepository.saveAndFlush(
        User.register(email, "second-hash", Instant.now())
    )).isInstanceOf(DataIntegrityViolationException.class);
  }
}
