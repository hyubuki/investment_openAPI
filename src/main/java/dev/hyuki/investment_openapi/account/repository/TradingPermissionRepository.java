package dev.hyuki.investment_openapi.account.repository;

import dev.hyuki.investment_openapi.account.entity.AssetClass;
import dev.hyuki.investment_openapi.account.entity.OrderType;
import dev.hyuki.investment_openapi.account.entity.PermissionSide;
import dev.hyuki.investment_openapi.account.entity.TradingPermission;
import dev.hyuki.investment_openapi.account.entity.TradingPermissionStatus;
import dev.hyuki.investment_openapi.account.entity.TradingMarket;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TradingPermissionRepository extends JpaRepository<TradingPermission, UUID> {

  @Query("""
      SELECT CASE WHEN COUNT(permission) > 0 THEN TRUE ELSE FALSE END
      FROM TradingPermission permission
      WHERE permission.accountId = :accountId
        AND permission.market = :market
        AND permission.assetClass = :assetClass
        AND permission.side IN :allowedSides
        AND permission.orderType = :orderType
        AND permission.status = :status
        AND permission.effectiveFrom <= :evaluatedAt
        AND (permission.effectiveUntil IS NULL OR permission.effectiveUntil > :evaluatedAt)
      """)
  boolean existsEffectivePermission(
      @Param("accountId") UUID accountId,
      @Param("market") TradingMarket market,
      @Param("assetClass") AssetClass assetClass,
      @Param("allowedSides") Set<PermissionSide> allowedSides,
      @Param("orderType") OrderType orderType,
      @Param("status") TradingPermissionStatus status,
      @Param("evaluatedAt") Instant evaluatedAt
  );
}
