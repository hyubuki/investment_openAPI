package dev.hyuki.investment_openapi.account.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "trading_accounts")
public class TradingAccount {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "account_id", nullable = false, updatable = false)
  private UUID accountId;

  @Column(name = "user_id", nullable = false, updatable = false)
  private UUID userId;

  @Column(name = "account_number_encrypted", nullable = false, length = 512)
  private String accountNumberEncrypted;

  @Column(name = "account_number_hash", nullable = false, updatable = false, length = 64)
  private String accountNumberHash;

  @Enumerated(EnumType.STRING)
  @Column(name = "account_type", nullable = false, length = 32)
  private AccountType accountType;

  @Column(name = "base_currency", nullable = false, length = 3)
  private String baseCurrency;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 32)
  private AccountStatus status;

  @Column(name = "opened_at", nullable = false, updatable = false)
  private Instant openedAt;

  @Column(name = "closed_at")
  private Instant closedAt;

  @Version
  @Column(name = "version", nullable = false)
  private long version;

  protected TradingAccount() {
  }

  private TradingAccount(
      UUID userId,
      String accountNumberEncrypted,
      String accountNumberHash,
      AccountType accountType,
      String baseCurrency,
      Instant openedAt
  ) {
    this.userId = Objects.requireNonNull(userId, "userId must not be null");
    this.accountNumberEncrypted = requireText(
        accountNumberEncrypted,
        "accountNumberEncrypted"
    );
    this.accountNumberHash = requireHash(accountNumberHash);
    this.accountType = Objects.requireNonNull(accountType, "accountType must not be null");
    this.baseCurrency = normalizeCurrency(baseCurrency);
    this.status = AccountStatus.PENDING;
    this.openedAt = Objects.requireNonNull(openedAt, "openedAt must not be null");
  }

  public static TradingAccount openPending(
      UUID userId,
      String accountNumberEncrypted,
      String accountNumberHash,
      AccountType accountType,
      String baseCurrency,
      Instant openedAt
  ) {
    return new TradingAccount(
        userId,
        accountNumberEncrypted,
        accountNumberHash,
        accountType,
        baseCurrency,
        openedAt
    );
  }

  public UUID getAccountId() {
    return accountId;
  }

  public UUID getUserId() {
    return userId;
  }

  @JsonIgnore
  public String getAccountNumberEncrypted() {
    return accountNumberEncrypted;
  }

  @JsonIgnore
  public String getAccountNumberHash() {
    return accountNumberHash;
  }

  public AccountType getAccountType() {
    return accountType;
  }

  public String getBaseCurrency() {
    return baseCurrency;
  }

  public AccountStatus getStatus() {
    return status;
  }

  public Instant getOpenedAt() {
    return openedAt;
  }

  public Instant getClosedAt() {
    return closedAt;
  }

  public long getVersion() {
    return version;
  }

  @Override
  public String toString() {
    return "TradingAccount[accountId=%s, userId=%s, accountType=%s, status=%s]"
        .formatted(accountId, userId, accountType, status);
  }

  private static String requireHash(String value) {
    String hash = requireText(value, "accountNumberHash");
    if (!hash.matches("[0-9a-f]{64}")) {
      throw new IllegalArgumentException("accountNumberHash must be 64 lowercase hex characters");
    }
    return hash;
  }

  private static String normalizeCurrency(String value) {
    String currency = requireText(value, "baseCurrency").toUpperCase(Locale.ROOT);
    if (!currency.matches("[A-Z]{3}")) {
      throw new IllegalArgumentException("baseCurrency must be a 3-letter currency code");
    }
    return currency;
  }

  private static String requireText(String value, String fieldName) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(fieldName + " must not be blank");
    }
    return value;
  }
}
