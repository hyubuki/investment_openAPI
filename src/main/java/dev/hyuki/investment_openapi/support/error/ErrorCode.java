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
