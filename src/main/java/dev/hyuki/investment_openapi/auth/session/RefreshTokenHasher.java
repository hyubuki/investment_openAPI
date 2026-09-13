package dev.hyuki.investment_openapi.auth.session;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class RefreshTokenHasher {

  public String hash(String refreshToken) {
    Objects.requireNonNull(refreshToken, "refreshToken must not be null");
    try {
      byte[] digest = MessageDigest.getInstance("SHA-256")
          .digest(refreshToken.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(digest);
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 is not available", exception);
    }
  }
}
