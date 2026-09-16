package dev.hyuki.investment_openapi.account.repository;

import static org.assertj.core.api.Assertions.assertThat;

import dev.hyuki.investment_openapi.account.entity.AccountType;
import dev.hyuki.investment_openapi.account.entity.AssetClass;
import dev.hyuki.investment_openapi.account.entity.OrderType;
import dev.hyuki.investment_openapi.account.entity.PermissionSide;
import dev.hyuki.investment_openapi.account.entity.TradingAccount;
import dev.hyuki.investment_openapi.account.entity.TradingPermission;
import dev.hyuki.investment_openapi.account.entity.TradingPermissionStatus;
import dev.hyuki.investment_openapi.account.entity.TradingMarket;
import dev.hyuki.investment_openapi.user.entity.User;
import dev.hyuki.investment_openapi.user.repository.UserRepository;
import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
class TradingPermissionRepositoryIntegrationTest {

  private static final Instant EFFECTIVE_FROM = Instant.parse("2026-09-15T00:00:00Z");
  private static final Instant EFFECTIVE_UNTIL = Instant.parse("2026-09-16T00:00:00Z");

  @Autowired
  private TradingPermissionRepository tradingPermissionRepository;

  @Autowired
  private TradingAccountRepository tradingAccountRepository;

  @Autowired
  private UserRepository userRepository;

  @BeforeEach
  void clearData() {
    tradingPermissionRepository.deleteAll();
    tradingAccountRepository.deleteAll();
    userRepository.deleteAll();
  }

  @Test
  @DisplayName("BOTH 방향의 활성 권한은 유효기간 안에서 매수와 매도 모두 허용한다")
  void findsEffectiveBothSidePermissionForBuyAndSell() {
    TradingAccount account = saveAccount("permission-owner@example.com", "a".repeat(64));
    tradingPermissionRepository.saveAndFlush(TradingPermission.grant(
        account.getAccountId(),
        TradingMarket.KR,
        AssetClass.EQUITY,
        PermissionSide.BOTH,
        OrderType.LIMIT,
        EFFECTIVE_FROM,
        EFFECTIVE_UNTIL
    ));

    assertThat(exists(account, PermissionSide.BUY, EFFECTIVE_FROM)).isTrue();
    assertThat(exists(account, PermissionSide.SELL, EFFECTIVE_UNTIL.minusNanos(1))).isTrue();
  }

  @Test
  @DisplayName("권한 종료 시각부터는 거래 권한이 유효하지 않다")
  void excludesPermissionAtEffectiveUntil() {
    TradingAccount account = saveAccount("expired-permission@example.com", "b".repeat(64));
    tradingPermissionRepository.saveAndFlush(TradingPermission.grant(
        account.getAccountId(),
        TradingMarket.KR,
        AssetClass.EQUITY,
        PermissionSide.BUY,
        OrderType.LIMIT,
        EFFECTIVE_FROM,
        EFFECTIVE_UNTIL
    ));

    assertThat(exists(account, PermissionSide.BUY, EFFECTIVE_UNTIL)).isFalse();
  }

  @Test
  @DisplayName("BUY 전용 권한은 같은 조건의 SELL 주문을 허용하지 않는다")
  void doesNotUseBuyOnlyPermissionForSellOrder() {
    TradingAccount account = saveAccount("buy-only-permission@example.com", "c".repeat(64));
    tradingPermissionRepository.saveAndFlush(TradingPermission.grant(
        account.getAccountId(),
        TradingMarket.KR,
        AssetClass.EQUITY,
        PermissionSide.BUY,
        OrderType.LIMIT,
        EFFECTIVE_FROM,
        EFFECTIVE_UNTIL
    ));

    assertThat(exists(account, PermissionSide.SELL, EFFECTIVE_FROM)).isFalse();
  }

  private boolean exists(
      TradingAccount account,
      PermissionSide requestedSide,
      Instant evaluatedAt
  ) {
    return tradingPermissionRepository.existsEffectivePermission(
        account.getAccountId(),
        TradingMarket.KR,
        AssetClass.EQUITY,
        Set.of(requestedSide, PermissionSide.BOTH),
        OrderType.LIMIT,
        TradingPermissionStatus.ACTIVE,
        evaluatedAt
    );
  }

  private TradingAccount saveAccount(String email, String accountNumberHash) {
    User owner = userRepository.saveAndFlush(User.register(
        email,
        "password-hash",
        EFFECTIVE_FROM
    ));
    return tradingAccountRepository.saveAndFlush(TradingAccount.openPending(
        owner.getUserId(),
        "v1.encrypted-account-number",
        accountNumberHash,
        AccountType.GENERAL_BROKERAGE,
        "KRW",
        EFFECTIVE_FROM
    ));
  }
}
