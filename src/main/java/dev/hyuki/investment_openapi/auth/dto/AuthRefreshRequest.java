package dev.hyuki.investment_openapi.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AuthRefreshRequest(
    @NotBlank
    @Size(max = 4096)
    String refreshToken
) {

  @Override
  public String toString() {
    return "AuthRefreshRequest[refreshToken=[REDACTED]]";
  }
}
