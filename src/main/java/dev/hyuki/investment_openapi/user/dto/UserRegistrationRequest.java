package dev.hyuki.investment_openapi.user.dto;

import dev.hyuki.investment_openapi.user.support.EmailNormalizer;
import dev.hyuki.investment_openapi.support.validation.Utf8ByteLength;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record UserRegistrationRequest(
    @NotBlank
    @Email
    @Utf8ByteLength(max = 100)
    String email,

    @NotBlank
    @Utf8ByteLength(min = 8, max = 72)
    String password
) {

  public UserRegistrationRequest {
    if (email != null) {
      email = EmailNormalizer.normalize(email);
    }
  }

  @Override
  public String toString() {
    return "UserRegistrationRequest[email=" + email + ", password=[REDACTED]]";
  }
}
