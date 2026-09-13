package dev.hyuki.investment_openapi.user.dto;

import dev.hyuki.investment_openapi.user.entity.UserRole;
import dev.hyuki.investment_openapi.user.entity.UserStatus;
import dev.hyuki.investment_openapi.user.service.RegisteredUser;
import java.time.Instant;
import java.util.UUID;

public record UserRegistrationResponse(
    UUID userId,
    String email,
    UserRole role,
    UserStatus status,
    Instant createdAt
) {

  public static UserRegistrationResponse from(RegisteredUser user) {
    return new UserRegistrationResponse(
        user.userId(),
        user.email(),
        user.role(),
        user.status(),
        user.createdAt()
    );
  }
}
