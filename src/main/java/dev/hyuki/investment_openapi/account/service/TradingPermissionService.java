package dev.hyuki.investment_openapi.account.service;

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
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TradingPermissionService {

  private final AccountAuthorizationService accountAuthorizationService;
  private final TradingPermissionRepository tradingPermissionRepository;
  private final Clock clock;

  public TradingPermissionService(
      AccountAuthorizationService accountAuthorizationService,
      TradingPermissionRepository tradingPermissionRepository,
      Clock clock
  ) {
    this.accountAuthorizationService = accountAuthorizationService;
    this.tradingPermissionRepository = tradingPermissionRepository;
    this.clock = clock;
  }

  @Transactional(readOnly = true)
  public TradingAccount requireOrderAllowed(
      UUID accountId,
      TradingMarket market,
      AssetClass assetClass,
      OrderSide requestedSide,
      OrderType orderType
  ) {
    Objects.requireNonNull(assetClass, "assetClass must not be null");
    Objects.requireNonNull(market, "market must not be null");
    Objects.requireNonNull(requestedSide, "requestedSide must not be null");
    Objects.requireNonNull(orderType, "orderType must not be null");
    TradingAccount account = accountAuthorizationService.requireOwnedAccount(accountId);
    if (account.getStatus() != AccountStatus.ACTIVE) {
      throw new ApiException(
          ErrorCode.ACCOUNT_NOT_ACTIVE,
          "The account is not active for new orders."
      );
    }

    Set<PermissionSide> allowedSides = Set.of(
        PermissionSide.forOrder(requestedSide),
        PermissionSide.BOTH
    );
    boolean allowed = tradingPermissionRepository.existsEffectivePermission(
        accountId,
        market,
        assetClass,
        allowedSides,
        orderType,
        TradingPermissionStatus.ACTIVE,
        clock.instant()
    );
    if (!allowed) {
      throw new ApiException(
          ErrorCode.TRADING_PERMISSION_DENIED,
          "The account does not have an effective permission for the requested order."
      );
    }
    return account;
  }
}
