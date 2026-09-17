package dev.hyuki.investment_openapi.account.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import dev.hyuki.investment_openapi.account.entity.AccountType;
import dev.hyuki.investment_openapi.account.entity.TradingAccount;
import dev.hyuki.investment_openapi.account.repository.TradingAccountRepository;
import dev.hyuki.investment_openapi.support.error.ApiException;
import dev.hyuki.investment_openapi.support.error.ErrorCode;
import dev.hyuki.investment_openapi.user.entity.User;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class AccountAuthorizationServiceTest {

  @Mock
  private TradingAccountRepository tradingAccountRepository;

  private AccountAuthorizationService accountAuthorizationService;

  @BeforeEach
  void setUp() {
    accountAuthorizationService = new AccountAuthorizationService(tradingAccountRepository);
  }

  @AfterEach
  void clearSecurityContext() {
    SecurityContextHolder.clearContext();
  }

  @Test
  @DisplayName("인증 사용자가 소유한 계좌이면 해당 계좌를 반환한다")
  void returnsAccountOwnedByAuthenticatedUser() {
    UUID userId = UUID.randomUUID();
    authenticate(userId);
    UUID accountId = UUID.randomUUID();
    TradingAccount account = account(userId);
    when(tradingAccountRepository.findByAccountIdAndUserId(accountId, userId))
        .thenReturn(Optional.of(account));

    TradingAccount result = accountAuthorizationService.requireOwnedAccount(accountId);

    assertThat(result).isSameAs(account);
  }

  @Test
  @DisplayName("계좌가 없거나 다른 사용자 소유이면 동일한 ACCOUNT_NOT_FOUND 오류를 반환한다")
  void hidesWhetherAccountExistsOutsideAuthenticatedUserScope() {
    UUID userId = UUID.randomUUID();
    authenticate(userId);
    UUID accountId = UUID.randomUUID();
    when(tradingAccountRepository.findByAccountIdAndUserId(accountId, userId))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> accountAuthorizationService.requireOwnedAccount(accountId))
        .isInstanceOfSatisfying(ApiException.class, exception -> {
          assertThat(exception.code()).isEqualTo(ErrorCode.ACCOUNT_NOT_FOUND);
          assertThat(exception.getMessage())
              .isEqualTo("The account was not found within the permitted scope.");
        });
  }

  @Test
  @DisplayName("인증 Principal이 없으면 계좌 조회 전에 UNAUTHORIZED 오류를 반환한다")
  void rejectsRequestWithoutAuthenticatedPrincipal() {
    assertThatThrownBy(() -> accountAuthorizationService.requireOwnedAccount(UUID.randomUUID()))
        .isInstanceOfSatisfying(ApiException.class, exception ->
            assertThat(exception.code()).isEqualTo(ErrorCode.UNAUTHORIZED)
        );
    verifyNoInteractions(tradingAccountRepository);
  }

  private void authenticate(UUID userId) {
    User principal = mock(User.class);
    when(principal.getUserId()).thenReturn(userId);
    SecurityContextHolder.getContext().setAuthentication(
        UsernamePasswordAuthenticationToken.authenticated(principal, null, List.of())
    );
  }

  private TradingAccount account(UUID userId) {
    return TradingAccount.openPending(
        userId,
        "v1.encrypted-account-number",
        "a".repeat(64),
        AccountType.GENERAL_BROKERAGE,
        "KRW",
        Instant.parse("2026-09-15T00:00:00Z")
    );
  }
}
