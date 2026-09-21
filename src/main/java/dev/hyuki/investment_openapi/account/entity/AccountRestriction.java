package dev.hyuki.investment_openapi.account.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "account_restrictions")
public class AccountRestriction {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "restriction_id", nullable = false, updatable = false)
  private UUID restrictionId;

  @Column(name = "account_id", nullable = false, updatable = false)
  private UUID accountId;

  @Enumerated(EnumType.STRING)
  @Column(name = "restriction_type", nullable = false, updatable = false, length = 32)
  private RestrictionType restrictionType;

  @Column(name = "reason_code", nullable = false, updatable = false, length = 64)
  private String reasonCode;

  @Column(name = "effective_from", nullable = false, updatable = false)
  private Instant effectiveFrom;

  @Column(name = "effective_until", updatable = false)
  private Instant effectiveUntil;

  @Column(name = "released_at")
  private Instant releasedAt;

  @Version
  @Column(name = "version", nullable = false)
  private long version;

  protected AccountRestriction() {
  }

  private AccountRestriction(
      UUID accountId,
      RestrictionType restrictionType,
      String reasonCode,
      Instant effectiveFrom,
      Instant effectiveUntil
  ) {
    this.accountId = Objects.requireNonNull(accountId, "accountId must not be null");
    this.restrictionType = Objects.requireNonNull(
        restrictionType,
        "restrictionType must not be null"
    );
    if (reasonCode == null || reasonCode.isBlank() || reasonCode.length() > 64) {
      throw new IllegalArgumentException("reasonCode must contain 1 to 64 characters");
    }
    this.reasonCode = reasonCode;
    this.effectiveFrom = Objects.requireNonNull(
        effectiveFrom,
        "effectiveFrom must not be null"
    );
    if (effectiveUntil != null && !effectiveUntil.isAfter(effectiveFrom)) {
      throw new IllegalArgumentException("effectiveUntil must be after effectiveFrom");
    }
    this.effectiveUntil = effectiveUntil;
  }

  public static AccountRestriction impose(
      UUID accountId,
      RestrictionType restrictionType,
      String reasonCode,
      Instant effectiveFrom,
      Instant effectiveUntil
  ) {
    return new AccountRestriction(
        accountId,
        restrictionType,
        reasonCode,
        effectiveFrom,
        effectiveUntil
    );
  }

  public void release(Instant releasedAt) {
    Objects.requireNonNull(releasedAt, "releasedAt must not be null");
    if (this.releasedAt != null) {
      throw new IllegalStateException("restriction has already been released");
    }
    this.releasedAt = releasedAt;
  }

  public UUID getRestrictionId() {
    return restrictionId;
  }

  public UUID getAccountId() {
    return accountId;
  }

  public RestrictionType getRestrictionType() {
    return restrictionType;
  }

  public String getReasonCode() {
    return reasonCode;
  }

  public Instant getEffectiveFrom() {
    return effectiveFrom;
  }

  public Instant getEffectiveUntil() {
    return effectiveUntil;
  }

  public Instant getReleasedAt() {
    return releasedAt;
  }

  public long getVersion() {
    return version;
  }
}
