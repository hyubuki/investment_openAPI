package dev.hyuki.investment_openapi.support.error;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
  VALIDATION_ERROR(
      HttpStatus.BAD_REQUEST,
      "validation-error",
      "Validation failed",
      false
  ),
  EMAIL_ALREADY_EXISTS(
      HttpStatus.CONFLICT,
      "email-already-exists",
      "Email already exists",
      false
  ),
  INVALID_CREDENTIALS(
      HttpStatus.UNAUTHORIZED,
      "invalid-credentials",
      "Invalid credentials",
      false
  ),
  USER_INACTIVE(
      HttpStatus.UNAUTHORIZED,
      "user-inactive",
      "User inactive",
      false
  ),
  UNAUTHORIZED(
      HttpStatus.UNAUTHORIZED,
      "unauthorized",
      "Unauthorized",
      false
  ),
  SESSION_INVALID(
      HttpStatus.UNAUTHORIZED,
      "session-invalid",
      "Session invalid",
      false
  ),
  REFRESH_TOKEN_REUSED(
      HttpStatus.UNAUTHORIZED,
      "refresh-token-reused",
      "Refresh token reused",
      false
  ),
  INVALID_TOKEN(
      HttpStatus.UNAUTHORIZED,
      "invalid-token",
      "Invalid token",
      false
  ),
  TOKEN_EXPIRED(
      HttpStatus.UNAUTHORIZED,
      "token-expired",
      "Token expired",
      false
  );

  private final HttpStatus status;
  private final String type;
  private final String title;
  private final boolean retryable;

  ErrorCode(HttpStatus status, String type, String title, boolean retryable) {
    this.status = status;
    this.type = type;
    this.title = title;
    this.retryable = retryable;
  }

  public HttpStatus status() {
    return status;
  }

  public String type() {
    return type;
  }

  public String title() {
    return title;
  }

  public boolean retryable() {
    return retryable;
  }
}
