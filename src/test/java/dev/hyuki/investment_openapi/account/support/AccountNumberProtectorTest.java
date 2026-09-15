package dev.hyuki.investment_openapi.account.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.hyuki.investment_openapi.account.config.AccountNumberProperties;
import dev.hyuki.investment_openapi.account.support.AccountNumberProtector.ProtectedAccountNumber;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AccountNumberProtectorTest {

  private static final String ENCRYPTION_KEY =
      "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";
  private static final String HASH_KEY =
      "MTExMTExMTExMTExMTExMTExMTExMTExMTExMTExMTE=";
  private static final String ACCOUNT_NUMBER = "123-4567-8901";
  private static final String NORMALIZED_ACCOUNT_NUMBER = "12345678901";

  private AccountNumberProtector protector;

  @BeforeEach
  void setUp() {
    protector = new AccountNumberProtector(
        new AccountNumberProperties(ENCRYPTION_KEY, HASH_KEY)
    );
  }

  @Test
  @DisplayName("계좌번호를 무작위 IV로 암호화하고 HMAC Hash와 마스킹 값으로 분리한다")
  void protectsAccountNumberWithRandomizedEncryptionHashAndMask() {
    ProtectedAccountNumber first = protector.protect(ACCOUNT_NUMBER);
    ProtectedAccountNumber second = protector.protect("  12345678901  ");

    assertThat(first.encrypted()).startsWith("v1.").isNotEqualTo(second.encrypted());
    assertThat(protector.decrypt(first.encrypted())).isEqualTo(NORMALIZED_ACCOUNT_NUMBER);
    assertThat(protector.decrypt(second.encrypted())).isEqualTo(NORMALIZED_ACCOUNT_NUMBER);
    assertThat(first.hash()).isEqualTo(second.hash()).hasSize(64);
    assertThat(first.masked()).isEqualTo("****-****-8901");
  }

  @Test
  @DisplayName("계좌번호 보호 결과의 문자열과 JSON에는 암호문과 Hash를 포함하지 않는다")
  void excludesEncryptedValueAndHashFromStringAndJson() throws Exception {
    ProtectedAccountNumber protectedAccountNumber = protector.protect(ACCOUNT_NUMBER);
    String json = new ObjectMapper().writeValueAsString(protectedAccountNumber);

    assertThat(protectedAccountNumber.toString())
        .contains(protectedAccountNumber.masked())
        .doesNotContain(protectedAccountNumber.encrypted())
        .doesNotContain(protectedAccountNumber.hash())
        .doesNotContain(NORMALIZED_ACCOUNT_NUMBER);
    assertThat(json)
        .contains(protectedAccountNumber.masked())
        .doesNotContain(
            "encrypted",
            "hash",
            protectedAccountNumber.encrypted(),
            protectedAccountNumber.hash(),
            NORMALIZED_ACCOUNT_NUMBER
        );
  }

  @Test
  @DisplayName("허용되지 않은 문자나 길이의 계좌번호는 변환하지 않는다")
  void rejectsInvalidAccountNumberFormat() {
    assertThatThrownBy(() -> protector.protect("1234A678901"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("account number must contain only digits and hyphens");
    assertThatThrownBy(() -> protector.protect("123-456"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("account number length must be between 8 and 14 digits");
    assertThatThrownBy(() -> protector.protect("123 4567 8901"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("account number must contain only digits and hyphens");
  }

  @Test
  @DisplayName("계좌번호 암호화 Key가 32바이트가 아니면 초기화를 거부한다")
  void rejectsInvalidEncryptionKeyLength() {
    AccountNumberProperties properties = new AccountNumberProperties("YQ==", HASH_KEY);

    assertThatThrownBy(() -> new AccountNumberProtector(properties))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("security.account-number.encryption-key must contain exactly 32 bytes");
  }
}
