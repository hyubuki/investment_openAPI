package dev.hyuki.investment_openapi.account.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "security.account-number")
public record AccountNumberProperties(
    @NotBlank String encryptionKey,
    @NotBlank String hashKey
) {
}
