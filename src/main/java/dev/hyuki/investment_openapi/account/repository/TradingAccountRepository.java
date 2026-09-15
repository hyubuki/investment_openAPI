package dev.hyuki.investment_openapi.account.repository;

import dev.hyuki.investment_openapi.account.entity.AccountStatus;
import dev.hyuki.investment_openapi.account.entity.TradingAccount;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TradingAccountRepository extends JpaRepository<TradingAccount, UUID> {

  Optional<TradingAccount> findByAccountNumberHash(String accountNumberHash);

  Optional<TradingAccount> findByAccountIdAndUserId(UUID accountId, UUID userId);

  Slice<TradingAccount> findAllByUserIdAndStatusOrderByAccountIdAsc(
      UUID userId,
      AccountStatus status,
      Pageable pageable
  );
}
