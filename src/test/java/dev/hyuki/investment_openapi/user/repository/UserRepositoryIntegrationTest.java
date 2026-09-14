package dev.hyuki.investment_openapi.user.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.hyuki.investment_openapi.user.entity.User;
import dev.hyuki.investment_openapi.user.entity.UserStatus;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@DataJpaTest
@ActiveProfiles("test")
class UserRepositoryIntegrationTest {

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private PlatformTransactionManager transactionManager;

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

  @Test
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  @DisplayName("동시 로그인 실패를 원자적으로 누적하고 임계치에서 한 번만 잠근다")
  void recordsConcurrentLoginFailuresAtomically() throws Exception {
    int maxFailedAttempts = 5;
    int concurrentAttempts = 10;
    String email = "concurrent-login@example.com";
    Instant failedAt = Instant.now().truncatedTo(ChronoUnit.MILLIS);
    Instant lockedUntil = failedAt.plusSeconds(900);
    userRepository.saveAndFlush(User.register(email, "password-hash", failedAt));
    ExecutorService executor = Executors.newFixedThreadPool(concurrentAttempts);
    TransactionTemplate transaction = new TransactionTemplate(transactionManager);

    try {
      List<Future<Integer>> results = IntStream.range(0, concurrentAttempts)
          .mapToObj(attempt -> executor.submit(() -> transaction.execute(status ->
              userRepository.recordLoginFailure(
                  email,
                  maxFailedAttempts,
                  lockedUntil,
                  failedAt
              )
          )))
          .toList();
      for (Future<Integer> result : results) {
        result.get();
      }
    } finally {
      executor.shutdownNow();
    }

    User lockedUser = userRepository.findByEmail(email).orElseThrow();
    assertThat(lockedUser.getFailedLoginCount()).isEqualTo(maxFailedAttempts);
    assertThat(lockedUser.getStatus()).isEqualTo(UserStatus.LOCKED);
    assertThat(lockedUser.getLockedUntil()).isEqualTo(lockedUntil);
  }
}
