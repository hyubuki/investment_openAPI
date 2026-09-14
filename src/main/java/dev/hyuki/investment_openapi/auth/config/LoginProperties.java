package dev.hyuki.investment_openapi.auth.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "security.login")
public record LoginProperties(
    @Min(1) int maxFailedAttempts,
    @NotNull Duration lockDuration
) {

  public LoginProperties {
    if (lockDuration != null && (lockDuration.isZero() || lockDuration.isNegative())) {
      throw new IllegalArgumentException("security.login.lock-duration must be positive");
    }
  }
}
