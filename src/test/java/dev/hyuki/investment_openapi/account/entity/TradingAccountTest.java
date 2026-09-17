package dev.hyuki.investment_openapi.account.entity;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TradingAccountTest {

  @Test
  @DisplayName("Entity 직렬화와 문자열 표현에서 계좌번호 암호문과 Hash를 노출하지 않는다")
  void excludesProtectedAccountNumberFromSerializationAndStringRepresentation() throws Exception {
    String encrypted = "v1.encrypted-account-number";
    String hash = "a".repeat(64);
    TradingAccount account = TradingAccount.openPending(
        UUID.randomUUID(),
        encrypted,
        hash,
        AccountType.GENERAL_BROKERAGE,
        "krw",
        Instant.parse("2026-09-15T00:00:00Z")
    );
    ObjectMapper objectMapper = JsonMapper.builder()
        .addModule(new JavaTimeModule())
        .build();

    String json = objectMapper.writeValueAsString(account);

    assertThat(json)
        .doesNotContain("accountNumberEncrypted", "accountNumberHash", encrypted, hash);
    assertThat(account.toString()).doesNotContain(encrypted, hash);
  }
}
