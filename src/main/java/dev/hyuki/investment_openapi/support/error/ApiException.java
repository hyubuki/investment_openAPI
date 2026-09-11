package dev.hyuki.investment_openapi.support.error;

public class ApiException extends RuntimeException {

  private final ErrorCode code;

  public ApiException(ErrorCode code, String message) {
    super(message);
    this.code = code;
  }

  public ApiException(ErrorCode code, String message, Throwable cause) {
    super(message, cause);
    this.code = code;
  }

  public ErrorCode code() {
    return code;
  }
}
