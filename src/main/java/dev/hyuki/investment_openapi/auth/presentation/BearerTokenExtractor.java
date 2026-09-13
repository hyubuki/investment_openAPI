package dev.hyuki.investment_openapi.auth.presentation;

import dev.hyuki.investment_openapi.support.error.ApiException;
import dev.hyuki.investment_openapi.support.error.ErrorCode;
import org.springframework.stereotype.Component;

@Component
public class BearerTokenExtractor {

  private static final String PREFIX = "Bearer ";

  public String extract(String authorizationHeader) {
    if (authorizationHeader == null
        || authorizationHeader.length() <= PREFIX.length()
        || !authorizationHeader.regionMatches(true, 0, PREFIX, 0, PREFIX.length())) {
      throw unauthorized();
    }
    String token = authorizationHeader.substring(PREFIX.length());
    if (token.isBlank()
        || !token.equals(token.trim())
        || token.chars().anyMatch(Character::isWhitespace)) {
      throw unauthorized();
    }
    return token;
  }

  private ApiException unauthorized() {
    return new ApiException(ErrorCode.UNAUTHORIZED, "A valid access token is required.");
  }
}
