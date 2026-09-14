package dev.hyuki.investment_openapi.user.dto;

import dev.hyuki.investment_openapi.user.entity.User;
import dev.hyuki.investment_openapi.user.entity.UserRole;
import dev.hyuki.investment_openapi.user.entity.UserStatus;
import java.time.Instant;
import java.util.UUID;

public record UserMeResponse(
    UUID userId,
    String email,
    UserRole role,
    UserStatus status,
    Instant createdAt
) {

  public static UserMeResponse from(User user) {
    return new UserMeResponse(
        user.getUserId(),
        user.getEmail(),
        user.getRole(),
        user.getStatus(),
        user.getCreatedAt()
    );
  }
}
