package dev.hyuki.investment_openapi.auth.dto;

import dev.hyuki.investment_openapi.support.validation.Utf8ByteLength;
import dev.hyuki.investment_openapi.user.support.EmailNormalizer;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record AuthLoginRequest(
    @NotBlank
    @Email
    @Utf8ByteLength(max = 100)
    String email,

    @NotBlank
    @Utf8ByteLength(max = 72)
    String password
) {

  public AuthLoginRequest {
    if (email != null) {
      email = EmailNormalizer.normalize(email);
    }
  }

  @Override
  public String toString() {
    return "AuthLoginRequest[email=" + email + ", password=[REDACTED]]";
  }
}
