package dev.hyuki.investment_openapi.auth.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "security.jwt")
public record JwtProperties(
    @NotBlank String issuer,
    @NotBlank String audience,
    @NotBlank String secret,
    @NotNull Duration accessTokenTtl,
    @NotNull Duration refreshTokenTtl
) {

  public JwtProperties {
    requirePositive(accessTokenTtl, "access-token-ttl");
    requirePositive(refreshTokenTtl, "refresh-token-ttl");
  }

  private static void requirePositive(Duration duration, String propertyName) {
    if (duration != null && (duration.isZero() || duration.isNegative())) {
      throw new IllegalArgumentException("security.jwt." + propertyName + " must be positive");
    }
  }
}
