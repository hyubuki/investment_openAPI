package dev.hyuki.investment_openapi.account.service;

import dev.hyuki.investment_openapi.account.entity.TradingAccount;
import dev.hyuki.investment_openapi.account.repository.TradingAccountRepository;
import dev.hyuki.investment_openapi.support.error.ApiException;
import dev.hyuki.investment_openapi.support.error.ErrorCode;
import dev.hyuki.investment_openapi.user.entity.User;
import java.util.Objects;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountAuthorizationService {

  private final TradingAccountRepository tradingAccountRepository;

  public AccountAuthorizationService(TradingAccountRepository tradingAccountRepository) {
    this.tradingAccountRepository = tradingAccountRepository;
  }

  @Transactional(readOnly = true)
  public TradingAccount requireOwnedAccount(UUID accountId) {
    Objects.requireNonNull(accountId, "accountId must not be null");
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !authentication.isAuthenticated()
        || !(authentication.getPrincipal() instanceof User user)
        || user.getUserId() == null) {
      throw new ApiException(ErrorCode.UNAUTHORIZED, "A valid access token is required.");
    }
    return tradingAccountRepository.findByAccountIdAndUserId(accountId, user.getUserId())
        .orElseThrow(() -> new ApiException(
            ErrorCode.ACCOUNT_NOT_FOUND,
            "The account was not found within the permitted scope."
        ));
  }
}
