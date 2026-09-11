package dev.hyuki.investment_openapi.user.support;

import java.util.Locale;
import java.util.Objects;

public final class EmailNormalizer {

  private EmailNormalizer() {
  }

  public static String normalize(String email) {
    return Objects.requireNonNull(email, "email must not be null")
        .trim()
        .toLowerCase(Locale.ROOT);
  }
}
