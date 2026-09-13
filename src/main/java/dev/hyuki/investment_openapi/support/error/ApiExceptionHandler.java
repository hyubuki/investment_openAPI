package dev.hyuki.investment_openapi.support.error;

import dev.hyuki.investment_openapi.support.filter.RequestIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.time.Clock;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

  private static final String PROBLEM_BASE_URL = "https://api.example.com/problems/";

  private final Clock clock;

  public ApiExceptionHandler(Clock clock) {
    this.clock = clock;
  }

  @ExceptionHandler(ApiException.class)
  public ResponseEntity<ProblemDetail> handleApiException(
      ApiException exception,
      HttpServletRequest request
  ) {
    return response(exception.code(), exception.getMessage(), List.of(), request);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ProblemDetail> handleValidation(
      MethodArgumentNotValidException exception,
      HttpServletRequest request
  ) {
    List<ValidationViolation> violations = exception.getBindingResult()
        .getFieldErrors()
        .stream()
        .map(error -> new ValidationViolation(error.getField(), error.getDefaultMessage()))
        .toList();
    return response(
        ErrorCode.VALIDATION_ERROR,
        "One or more request fields are invalid.",
        violations,
        request
    );
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ProblemDetail> handleUnreadableBody(
      HttpMessageNotReadableException exception,
      HttpServletRequest request
  ) {
    return response(
        ErrorCode.VALIDATION_ERROR,
        "The request body is missing or malformed.",
        List.of(),
        request
    );
  }

  private ResponseEntity<ProblemDetail> response(
      ErrorCode code,
      String detail,
      List<ValidationViolation> violations,
      HttpServletRequest request
  ) {
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(code.status(), detail);
    problem.setType(URI.create(PROBLEM_BASE_URL + code.type()));
    problem.setTitle(code.title());
    problem.setInstance(URI.create(request.getRequestURI()));
    problem.setProperty("code", code.name());
    problem.setProperty("requestId", request.getAttribute(RequestIdFilter.REQUEST_ID_ATTRIBUTE));
    problem.setProperty("retryable", code.retryable());
    problem.setProperty("occurredAt", clock.instant());
    problem.setProperty("violations", violations);
    return ResponseEntity.status(code.status())
        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
        .body(problem);
  }
}
