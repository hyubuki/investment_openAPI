package dev.hyuki.investment_openapi.account.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "trading_permissions")
public class TradingPermission {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "permission_id", nullable = false, updatable = false)
  private UUID permissionId;

  @Column(name = "account_id", nullable = false, updatable = false)
  private UUID accountId;

  @Enumerated(EnumType.STRING)
  @Column(name = "market", nullable = false, updatable = false, length = 16)
  private TradingMarket market;

  @Enumerated(EnumType.STRING)
  @Column(name = "asset_class", nullable = false, updatable = false, length = 32)
  private AssetClass assetClass;

  @Enumerated(EnumType.STRING)
  @Column(name = "side", nullable = false, updatable = false, length = 16)
  private PermissionSide side;

  @Enumerated(EnumType.STRING)
  @Column(name = "order_type", nullable = false, updatable = false, length = 32)
  private OrderType orderType;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 32)
  private TradingPermissionStatus status;

  @Column(name = "effective_from", nullable = false, updatable = false)
  private Instant effectiveFrom;

  @Column(name = "effective_until")
  private Instant effectiveUntil;

  protected TradingPermission() {
  }

  private TradingPermission(
      UUID accountId,
      TradingMarket market,
      AssetClass assetClass,
      PermissionSide side,
      OrderType orderType,
      Instant effectiveFrom,
      Instant effectiveUntil
  ) {
    this.accountId = Objects.requireNonNull(accountId, "accountId must not be null");
    this.market = Objects.requireNonNull(market, "market must not be null");
    this.assetClass = Objects.requireNonNull(assetClass, "assetClass must not be null");
    this.side = Objects.requireNonNull(side, "side must not be null");
    this.orderType = Objects.requireNonNull(orderType, "orderType must not be null");
    this.status = TradingPermissionStatus.ACTIVE;
    this.effectiveFrom = Objects.requireNonNull(
        effectiveFrom,
        "effectiveFrom must not be null"
    );
    if (effectiveUntil != null && !effectiveUntil.isAfter(effectiveFrom)) {
      throw new IllegalArgumentException("effectiveUntil must be after effectiveFrom");
    }
    this.effectiveUntil = effectiveUntil;
  }

  public static TradingPermission grant(
      UUID accountId,
      TradingMarket market,
      AssetClass assetClass,
      PermissionSide side,
      OrderType orderType,
      Instant effectiveFrom,
      Instant effectiveUntil
  ) {
    return new TradingPermission(
        accountId,
        market,
        assetClass,
        side,
        orderType,
        effectiveFrom,
        effectiveUntil
    );
  }

  public UUID getPermissionId() {
    return permissionId;
  }

  public UUID getAccountId() {
    return accountId;
  }

  public TradingMarket getMarket() {
    return market;
  }

  public AssetClass getAssetClass() {
    return assetClass;
  }

  public PermissionSide getSide() {
    return side;
  }

  public OrderType getOrderType() {
    return orderType;
  }

  public TradingPermissionStatus getStatus() {
    return status;
  }

  public Instant getEffectiveFrom() {
    return effectiveFrom;
  }

  public Instant getEffectiveUntil() {
    return effectiveUntil;
  }
}
