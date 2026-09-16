package dev.hyuki.investment_openapi.account.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import dev.hyuki.investment_openapi.account.entity.AccountStatus;
import dev.hyuki.investment_openapi.account.entity.AssetClass;
import dev.hyuki.investment_openapi.account.entity.OrderSide;
import dev.hyuki.investment_openapi.account.entity.OrderType;
import dev.hyuki.investment_openapi.account.entity.PermissionSide;
import dev.hyuki.investment_openapi.account.entity.TradingAccount;
import dev.hyuki.investment_openapi.account.entity.TradingMarket;
import dev.hyuki.investment_openapi.account.entity.TradingPermissionStatus;
import dev.hyuki.investment_openapi.account.repository.TradingPermissionRepository;
import dev.hyuki.investment_openapi.support.error.ApiException;
import dev.hyuki.investment_openapi.support.error.ErrorCode;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TradingPermissionServiceTest {

  private static final Instant NOW = Instant.parse("2026-09-15T01:00:00Z");

  @Mock
  private AccountAuthorizationService accountAuthorizationService;

  @Mock
  private TradingPermissionRepository tradingPermissionRepository;

  private TradingPermissionService tradingPermissionService;

  @BeforeEach
  void setUp() {
    tradingPermissionService = new TradingPermissionService(
        accountAuthorizationService,
        tradingPermissionRepository,
        Clock.fixed(NOW, ZoneOffset.UTC)
    );
  }

  @Test
  @DisplayName("본인 ACTIVE 계좌에 유효한 BOTH 권한이 있으면 매수 주문을 허용한다")
  void allowsOrderWithEffectivePermission() {
    UUID accountId = UUID.randomUUID();
    TradingAccount account = accountWithStatus(AccountStatus.ACTIVE);
    when(accountAuthorizationService.requireOwnedAccount(accountId)).thenReturn(account);
    when(tradingPermissionRepository.existsEffectivePermission(
        accountId,
        TradingMarket.KR,
        AssetClass.EQUITY,
        Set.of(PermissionSide.BUY, PermissionSide.BOTH),
        OrderType.LIMIT,
        TradingPermissionStatus.ACTIVE,
        NOW
    )).thenReturn(true);

    TradingAccount result = tradingPermissionService.requireOrderAllowed(
        accountId,
        TradingMarket.KR,
        AssetClass.EQUITY,
        OrderSide.BUY,
        OrderType.LIMIT
    );

    assertThat(result).isSameAs(account);
  }

  @Test
  @DisplayName("본인 계좌라도 ACTIVE 상태가 아니면 권한 조회 전에 주문을 거부한다")
  void rejectsOrderWhenAccountIsNotActive() {
    UUID accountId = UUID.randomUUID();
    TradingAccount account = accountWithStatus(AccountStatus.RESTRICTED);
    when(accountAuthorizationService.requireOwnedAccount(accountId)).thenReturn(account);

    assertThatThrownBy(() -> tradingPermissionService.requireOrderAllowed(
        accountId,
        TradingMarket.KR,
        AssetClass.EQUITY,
        OrderSide.SELL,
        OrderType.MARKET
    )).isInstanceOfSatisfying(ApiException.class, exception ->
        assertThat(exception.code()).isEqualTo(ErrorCode.ACCOUNT_NOT_ACTIVE)
    );
    verifyNoInteractions(tradingPermissionRepository);
  }

  @Test
  @DisplayName("현재 유효한 거래 권한이 없으면 TRADING_PERMISSION_DENIED 오류를 반환한다")
  void rejectsOrderWithoutEffectivePermission() {
    UUID accountId = UUID.randomUUID();
    TradingAccount account = accountWithStatus(AccountStatus.ACTIVE);
    when(accountAuthorizationService.requireOwnedAccount(accountId)).thenReturn(account);
    when(tradingPermissionRepository.existsEffectivePermission(
        accountId,
        TradingMarket.KR,
        AssetClass.EQUITY,
        Set.of(PermissionSide.SELL, PermissionSide.BOTH),
        OrderType.MARKET,
        TradingPermissionStatus.ACTIVE,
        NOW
    )).thenReturn(false);

    assertThatThrownBy(() -> tradingPermissionService.requireOrderAllowed(
        accountId,
        TradingMarket.KR,
        AssetClass.EQUITY,
        OrderSide.SELL,
        OrderType.MARKET
    )).isInstanceOfSatisfying(ApiException.class, exception ->
        assertThat(exception.code()).isEqualTo(ErrorCode.TRADING_PERMISSION_DENIED)
    );
  }

  private TradingAccount accountWithStatus(AccountStatus status) {
    TradingAccount account = mock(TradingAccount.class);
    when(account.getStatus()).thenReturn(status);
    return account;
  }
}
