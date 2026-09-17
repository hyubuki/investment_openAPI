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
  FORBIDDEN(
      HttpStatus.FORBIDDEN,
      "forbidden",
      "Forbidden",
      false
  ),
  AUTHENTICATION_UNAVAILABLE(
      HttpStatus.SERVICE_UNAVAILABLE,
      "authentication-unavailable",
      "Authentication unavailable",
      true
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
  ),
  ACCOUNT_NOT_FOUND(
      HttpStatus.NOT_FOUND,
      "account-not-found",
      "Account not found",
      false
  ),
  ACCOUNT_NOT_ACTIVE(
      HttpStatus.UNPROCESSABLE_ENTITY,
      "account-not-active",
      "Account not active",
      false
  ),
  TRADING_PERMISSION_DENIED(
      HttpStatus.UNPROCESSABLE_ENTITY,
      "trading-permission-denied",
      "Trading permission denied",
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
