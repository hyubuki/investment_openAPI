package dev.hyuki.investment_openapi.account.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.hyuki.investment_openapi.account.entity.AccountStatus;
import dev.hyuki.investment_openapi.account.entity.AccountType;
import dev.hyuki.investment_openapi.account.entity.TradingAccount;
import dev.hyuki.investment_openapi.user.entity.User;
import dev.hyuki.investment_openapi.user.repository.UserRepository;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
class TradingAccountRepositoryIntegrationTest {

  @Autowired
  private TradingAccountRepository tradingAccountRepository;

  @Autowired
  private UserRepository userRepository;

  @BeforeEach
  void clearData() {
    tradingAccountRepository.deleteAll();
    userRepository.deleteAll();
  }

  @Test
  @DisplayName("TradingAccount는 User 객체 대신 userId로 소유 관계와 PENDING 상태를 저장한다")
  void persistsAccountOwnershipAsUserIdWithoutEntityAssociation() {
    User owner = saveUser("owner@example.com");
    TradingAccount account = tradingAccountRepository.saveAndFlush(account(
        owner,
        "v1.encrypted-account-number",
        "a".repeat(64)
    ));

    assertThat(account.getAccountId()).isNotNull();
    assertThat(account.getUserId()).isEqualTo(owner.getUserId());
    assertThat(account.getStatus()).isEqualTo(AccountStatus.PENDING);
    assertThat(account.getBaseCurrency()).isEqualTo("KRW");
    assertThat(account.getVersion()).isZero();
    assertThat(tradingAccountRepository.findByAccountIdAndUserId(
        account.getAccountId(),
        owner.getUserId()
    )).contains(account);
    assertThat(tradingAccountRepository.findAllByUserIdAndStatusOrderByAccountIdAsc(
        owner.getUserId(),
        AccountStatus.PENDING,
        PageRequest.of(0, 20)
    ).getContent()).containsExactly(account);
  }

  @Test
  @DisplayName("검색용 계좌번호 Hash가 중복되면 데이터베이스가 저장을 거부한다")
  void rejectsDuplicateAccountNumberHash() {
    User owner = saveUser("duplicate-account@example.com");
    String duplicateHash = "b".repeat(64);
    tradingAccountRepository.saveAndFlush(account(owner, "v1.first", duplicateHash));

    assertThatThrownBy(() -> tradingAccountRepository.saveAndFlush(
        account(owner, "v1.second", duplicateHash)
    )).isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  @DisplayName("중개형 ISA 계좌는 일반 위탁계좌와 다른 계좌 유형으로 저장한다")
  void persistsIsaBrokerageAccountType() {
    User owner = saveUser("isa-owner@example.com");
    TradingAccount account = tradingAccountRepository.saveAndFlush(TradingAccount.openPending(
        owner.getUserId(),
        "v1.isa-encrypted-account-number",
        "c".repeat(64),
        AccountType.ISA_BROKERAGE,
        "KRW",
        Instant.parse("2026-09-15T00:00:00Z")
    ));

    assertThat(tradingAccountRepository.findById(account.getAccountId()))
        .get()
        .extracting(TradingAccount::getAccountType)
        .isEqualTo(AccountType.ISA_BROKERAGE);
  }

  private User saveUser(String email) {
    return userRepository.saveAndFlush(User.register(
        email,
        "password-hash",
        Instant.parse("2026-09-15T00:00:00Z")
    ));
  }

  private TradingAccount account(User owner, String encrypted, String hash) {
    return TradingAccount.openPending(
        owner.getUserId(),
        encrypted,
        hash,
        AccountType.GENERAL_BROKERAGE,
        "krw",
        Instant.parse("2026-09-15T00:00:00Z")
    );
  }
}
