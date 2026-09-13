package dev.hyuki.investment_openapi.user.dto;

import dev.hyuki.investment_openapi.auth.dto.AuthTokensResponse;
import dev.hyuki.investment_openapi.user.entity.UserRole;
import dev.hyuki.investment_openapi.user.entity.UserStatus;
import dev.hyuki.investment_openapi.user.service.RegisteredUserSession;
import java.time.Instant;
import java.util.UUID;

public record UserRegistrationResponse(
    UUID userId,
    String email,
    UserRole role,
    UserStatus status,
    Instant createdAt,
    AuthTokensResponse tokens
) {

  public static UserRegistrationResponse from(RegisteredUserSession registration) {
    var user = registration.user();
    return new UserRegistrationResponse(
        user.userId(),
        user.email(),
        user.role(),
        user.status(),
        user.createdAt(),
        AuthTokensResponse.from(registration.tokens())
    );
  }
}
