package dev.hyuki.investment_openapi.account.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TradingPermissionTest {

  private static final Instant EFFECTIVE_FROM = Instant.parse("2026-09-15T00:00:00Z");

  @Test
  @DisplayName("거래 권한을 생성하면 거래 시장과 ACTIVE 상태를 저장한다")
  void grantsActivePermissionForTradingMarket() {
    UUID accountId = UUID.randomUUID();

    TradingPermission permission = TradingPermission.grant(
        accountId,
        TradingMarket.KR,
        AssetClass.EQUITY,
        PermissionSide.BOTH,
        OrderType.LIMIT,
        EFFECTIVE_FROM,
        null
    );

    assertThat(permission.getAccountId()).isEqualTo(accountId);
    assertThat(permission.getMarket()).isEqualTo(TradingMarket.KR);
    assertThat(permission.getStatus()).isEqualTo(TradingPermissionStatus.ACTIVE);
    assertThat(permission.getEffectiveUntil()).isNull();
  }

  @Test
  @DisplayName("거래 권한 종료 시각은 시작 시각보다 뒤여야 한다")
  void rejectsInvalidEffectivePeriod() {
    assertThatThrownBy(() -> TradingPermission.grant(
        UUID.randomUUID(),
        TradingMarket.KR,
        AssetClass.EQUITY,
        PermissionSide.BUY,
        OrderType.MARKET,
        EFFECTIVE_FROM,
        EFFECTIVE_FROM
    ))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("effectiveUntil must be after effectiveFrom");
  }

  @Test
  @DisplayName("실제 주문 방향은 BUY·SELL만 표현하고 BOTH는 권한 범위에만 둔다")
  void mapsOrderSideToPermissionSide() {
    assertThat(OrderSide.values()).containsExactly(OrderSide.BUY, OrderSide.SELL);
    assertThat(PermissionSide.forOrder(OrderSide.BUY)).isEqualTo(PermissionSide.BUY);
    assertThat(PermissionSide.forOrder(OrderSide.SELL)).isEqualTo(PermissionSide.SELL);
  }
}
