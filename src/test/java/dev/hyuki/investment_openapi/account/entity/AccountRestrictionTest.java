package dev.hyuki.investment_openapi.account.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AccountRestrictionTest {

  private static final Instant START = Instant.parse("2026-09-17T00:00:00Z");

  @Test
  @DisplayName("계좌 제한을 생성하면 제한 방향, 사유 코드와 적용 시각을 보관한다")
  void imposesRestriction() {
    UUID accountId = UUID.randomUUID();

    AccountRestriction restriction = AccountRestriction.impose(
        accountId,
        RestrictionType.BUY_BLOCKED,
        "RISK_REVIEW",
        START,
        null
    );

    assertThat(restriction.getAccountId()).isEqualTo(accountId);
    assertThat(restriction.getRestrictionType()).isEqualTo(RestrictionType.BUY_BLOCKED);
    assertThat(restriction.getReasonCode()).isEqualTo("RISK_REVIEW");
    assertThat(restriction.getReleasedAt()).isNull();
  }

  @Test
  @DisplayName("종료 시각이 시작 시각 이후가 아니면 계좌 제한을 생성할 수 없다")
  void rejectsInvalidPeriod() {
    assertThatThrownBy(() -> AccountRestriction.impose(
        UUID.randomUUID(),
        RestrictionType.ALL_TRADING_BLOCKED,
        "RISK_REVIEW",
        START,
        START
    )).isInstanceOf(IllegalArgumentException.class)
        .hasMessage("effectiveUntil must be after effectiveFrom");
  }

  @Test
  @DisplayName("해제된 제한을 다시 해제하면 기존 해제 시각을 덮어쓰지 않는다")
  void doesNotOverwriteReleaseTime() {
    AccountRestriction restriction = AccountRestriction.impose(
        UUID.randomUUID(),
        RestrictionType.SELL_BLOCKED,
        "RISK_REVIEW",
        START,
        null
    );
    Instant releasedAt = START.plusSeconds(60);
    restriction.release(releasedAt);

    assertThatThrownBy(() -> restriction.release(START.plusSeconds(120)))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("restriction has already been released");
    assertThat(restriction.getReleasedAt()).isEqualTo(releasedAt);
  }
}
