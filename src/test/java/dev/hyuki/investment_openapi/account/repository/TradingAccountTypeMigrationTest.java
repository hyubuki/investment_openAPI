package dev.hyuki.investment_openapi.account.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class TradingAccountTypeMigrationTest {

  @Test
  @DisplayName("기존 BROKERAGE 계좌는 V4 적용 후 일반 위탁계좌로 보존된다")
  void migratesExistingBrokerageAccount() {
    String databaseName = "account_type_migration_" + UUID.randomUUID().toString().replace("-", "");
    DriverManagerDataSource dataSource = new DriverManagerDataSource(
        "jdbc:h2:mem:" + databaseName + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "sa",
        ""
    );
    dataSource.setDriverClassName("org.h2.Driver");
    Flyway.configure().dataSource(dataSource).target("2").load().migrate();

    JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
    UUID userId = UUID.randomUUID();
    UUID accountId = UUID.randomUUID();
    Instant openedAt = Instant.parse("2026-09-15T00:00:00Z");
    jdbcTemplate.update("""
        INSERT INTO users (
            user_id, email, password_hash, role, status,
            failed_login_count, created_at, updated_at
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """, userId, "migration@example.com", "password-hash", "USER", "ACTIVE",
        0, openedAt, openedAt);
    jdbcTemplate.update("""
        INSERT INTO trading_accounts (
            account_id, user_id, account_number_encrypted, account_number_hash,
            account_type, base_currency, status, opened_at, version
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """, accountId, userId, "v1.encrypted-account-number", "a".repeat(64),
        "BROKERAGE", "KRW", "ACTIVE", openedAt, 0);

    Flyway.configure().dataSource(dataSource).load().migrate();

    assertThat(jdbcTemplate.queryForObject(
        "SELECT account_type FROM trading_accounts WHERE account_id = ?",
        String.class,
        accountId
    )).isEqualTo("GENERAL_BROKERAGE");
  }
}
