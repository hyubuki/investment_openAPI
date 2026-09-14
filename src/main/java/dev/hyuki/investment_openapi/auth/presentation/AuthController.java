package dev.hyuki.investment_openapi.auth.presentation;

import dev.hyuki.investment_openapi.auth.dto.AuthLoginRequest;
import dev.hyuki.investment_openapi.auth.dto.AuthRefreshRequest;
import dev.hyuki.investment_openapi.auth.dto.AuthTokensResponse;
import dev.hyuki.investment_openapi.auth.filter.BearerTokenExtractor;
import dev.hyuki.investment_openapi.auth.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

  private final AuthService authService;
  private final BearerTokenExtractor bearerTokenExtractor;

  public AuthController(AuthService authService, BearerTokenExtractor bearerTokenExtractor) {
    this.authService = authService;
    this.bearerTokenExtractor = bearerTokenExtractor;
  }

  @PostMapping("/login")
  public ResponseEntity<AuthTokensResponse> login(
      @Valid @RequestBody AuthLoginRequest request
  ) {
    return ResponseEntity.ok(AuthTokensResponse.from(
        authService.login(request.email(), request.password())
    ));
  }

  @PostMapping("/refresh")
  public ResponseEntity<AuthTokensResponse> refresh(
      @Valid @RequestBody AuthRefreshRequest request
  ) {
    return ResponseEntity.ok(AuthTokensResponse.from(
        authService.refresh(request.refreshToken())
    ));
  }

  @PostMapping("/logout")
  public ResponseEntity<Void> logout(
      @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization
  ) {
    authService.logout(bearerTokenExtractor.extract(authorization));
    return ResponseEntity.noContent().build();
  }
}
