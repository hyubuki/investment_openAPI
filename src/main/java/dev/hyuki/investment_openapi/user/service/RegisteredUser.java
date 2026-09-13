package dev.hyuki.investment_openapi.user.service;

import dev.hyuki.investment_openapi.user.entity.User;
import dev.hyuki.investment_openapi.user.entity.UserRole;
import dev.hyuki.investment_openapi.user.entity.UserStatus;
import java.time.Instant;
import java.util.UUID;

public record RegisteredUser(
    UUID userId,
    String email,
    UserRole role,
    UserStatus status,
    Instant createdAt
) {

  static RegisteredUser from(User user) {
    return new RegisteredUser(
        user.getUserId(),
        user.getEmail(),
        user.getRole(),
        user.getStatus(),
        user.getCreatedAt()
    );
  }
}
