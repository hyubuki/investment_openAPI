package dev.hyuki.investment_openapi.account.repository;

import static org.assertj.core.api.Assertions.assertThat;

import dev.hyuki.investment_openapi.account.entity.AccountRestriction;
import dev.hyuki.investment_openapi.account.entity.AccountType;
import dev.hyuki.investment_openapi.account.entity.RestrictionType;
import dev.hyuki.investment_openapi.account.entity.TradingAccount;
import dev.hyuki.investment_openapi.user.entity.User;
import dev.hyuki.investment_openapi.user.repository.UserRepository;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
class AccountRestrictionRepositoryIntegrationTest {

  private static final Instant START = Instant.parse("2026-09-17T00:00:00Z");
  private static final Instant END = START.plusSeconds(3600);

  @Autowired
  private AccountRestrictionRepository accountRestrictionRepository;

  @Autowired
  private TradingAccountRepository tradingAccountRepository;

  @Autowired
  private UserRepository userRepository;

  @Test
  @DisplayName("제한은 적용 시작 시각에 활성이고 종료 시각부터 비활성이다")
  void evaluatesHalfOpenPeriod() {
    UUID accountId = saveAccount("restriction-period@example.com", "d".repeat(64));
    accountRestrictionRepository.saveAndFlush(AccountRestriction.impose(
        accountId, RestrictionType.ALL_TRADING_BLOCKED, "RISK_REVIEW", START, END
    ));

    assertThat(exists(accountId, Set.of(RestrictionType.ALL_TRADING_BLOCKED), START)).isTrue();
    assertThat(exists(accountId, Set.of(RestrictionType.ALL_TRADING_BLOCKED), END)).isFalse();
  }

  @Test
  @DisplayName("해제된 제한은 해제 시각부터 비활성이다")
  void excludesReleasedRestriction() {
    UUID accountId = saveAccount("restriction-release@example.com", "e".repeat(64));
    AccountRestriction restriction = accountRestrictionRepository.saveAndFlush(
        AccountRestriction.impose(
            accountId, RestrictionType.BUY_BLOCKED, "RISK_REVIEW", START, END
        )
    );
    Instant releasedAt = START.plusSeconds(300);
    long initialVersion = restriction.getVersion();
    restriction.release(releasedAt);
    accountRestrictionRepository.flush();

    assertThat(restriction.getVersion()).isEqualTo(initialVersion + 1);
    assertThat(exists(accountId, Set.of(RestrictionType.BUY_BLOCKED), START)).isTrue();
    assertThat(exists(accountId, Set.of(RestrictionType.BUY_BLOCKED), releasedAt)).isFalse();
  }

  @Test
  @DisplayName("다른 계좌의 제한과 출금 제한은 매도 주문 제한에 포함되지 않는다")
  void scopesRestrictionByAccountAndType() {
    UUID accountId = saveAccount("restriction-owner@example.com", "f".repeat(64));
    UUID anotherAccountId = saveAccount("restriction-other@example.com", "1".repeat(64));
    accountRestrictionRepository.saveAndFlush(AccountRestriction.impose(
        accountId, RestrictionType.WITHDRAWAL_BLOCKED, "RISK_REVIEW", START, null
    ));
    accountRestrictionRepository.saveAndFlush(AccountRestriction.impose(
        anotherAccountId, RestrictionType.SELL_BLOCKED, "RISK_REVIEW", START, null
    ));

    assertThat(exists(accountId, Set.of(
        RestrictionType.SELL_BLOCKED, RestrictionType.ALL_TRADING_BLOCKED
    ), START)).isFalse();
    assertThat(exists(anotherAccountId, Set.of(
        RestrictionType.SELL_BLOCKED, RestrictionType.ALL_TRADING_BLOCKED
    ), START)).isTrue();
  }

  private boolean exists(UUID accountId, Set<RestrictionType> types, Instant evaluatedAt) {
    return accountRestrictionRepository.existsActiveRestriction(accountId, types, evaluatedAt);
  }

  private UUID saveAccount(String email, String accountNumberHash) {
    User owner = userRepository.saveAndFlush(User.register(email, "password-hash", START));
    TradingAccount account = tradingAccountRepository.saveAndFlush(TradingAccount.openPending(
        owner.getUserId(),
        "v1.encrypted-account-number",
        accountNumberHash,
        AccountType.GENERAL_BROKERAGE,
        "KRW",
        START
    ));
    return account.getAccountId();
  }
}
