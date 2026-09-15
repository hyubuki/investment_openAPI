package dev.hyuki.investment_openapi.account.support;

import com.fasterxml.jackson.annotation.JsonIgnore;
import dev.hyuki.investment_openapi.account.config.AccountNumberProperties;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Objects;
import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

@Component
public class AccountNumberProtector {

  private static final String CIPHER_TRANSFORMATION = "AES/GCM/NoPadding";
  private static final String HMAC_ALGORITHM = "HmacSHA256";
  private static final String VERSION_PREFIX = "v1.";
  private static final byte[] ADDITIONAL_AUTHENTICATED_DATA =
      "investment-openapi:account-number:v1".getBytes(StandardCharsets.US_ASCII);
  private static final String HASH_CONTEXT = "ACCOUNT:v1:";
  private static final int AES_KEY_BYTES = 32;
  private static final int MINIMUM_HMAC_KEY_BYTES = 32;
  private static final int GCM_IV_BYTES = 12;
  private static final int GCM_TAG_BITS = 128;
  private static final int MINIMUM_ACCOUNT_NUMBER_LENGTH = 8;
  private static final int MAXIMUM_ACCOUNT_NUMBER_LENGTH = 14;

  private final SecretKey encryptionKey;
  private final SecretKey hashKey;
  private final SecureRandom secureRandom;

  public AccountNumberProtector(AccountNumberProperties properties) {
    Objects.requireNonNull(properties, "properties must not be null");
    this.encryptionKey = encryptionKey(properties.encryptionKey());
    this.hashKey = hashKey(properties.hashKey());
    this.secureRandom = new SecureRandom();
  }

  public ProtectedAccountNumber protect(String accountNumber) {
    String normalized = normalize(accountNumber);
    return new ProtectedAccountNumber(
        encryptNormalized(normalized),
        hashNormalized(normalized),
        maskNormalized(normalized)
    );
  }

  public String hash(String accountNumber) {
    return hashNormalized(normalize(accountNumber));
  }

  public String mask(String accountNumber) {
    return maskNormalized(normalize(accountNumber));
  }

  public String decrypt(String encryptedAccountNumber) {
    if (encryptedAccountNumber == null || !encryptedAccountNumber.startsWith(VERSION_PREFIX)) {
      throw new IllegalArgumentException("encrypted account number format is invalid");
    }
    try {
      byte[] payload = Base64.getUrlDecoder().decode(
          encryptedAccountNumber.substring(VERSION_PREFIX.length())
      );
      if (payload.length <= GCM_IV_BYTES) {
        throw new IllegalArgumentException("encrypted account number format is invalid");
      }
      ByteBuffer buffer = ByteBuffer.wrap(payload);
      byte[] iv = new byte[GCM_IV_BYTES];
      buffer.get(iv);
      byte[] ciphertext = new byte[buffer.remaining()];
      buffer.get(ciphertext);

      Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
      cipher.init(Cipher.DECRYPT_MODE, encryptionKey, new GCMParameterSpec(GCM_TAG_BITS, iv));
      cipher.updateAAD(ADDITIONAL_AUTHENTICATED_DATA);
      return new String(cipher.doFinal(ciphertext), StandardCharsets.US_ASCII);
    } catch (IllegalArgumentException exception) {
      throw exception;
    } catch (GeneralSecurityException exception) {
      throw new IllegalArgumentException("encrypted account number could not be decrypted", exception);
    }
  }

  private String encryptNormalized(String normalized) {
    try {
      byte[] iv = new byte[GCM_IV_BYTES];
      secureRandom.nextBytes(iv);
      Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
      cipher.init(Cipher.ENCRYPT_MODE, encryptionKey, new GCMParameterSpec(GCM_TAG_BITS, iv));
      cipher.updateAAD(ADDITIONAL_AUTHENTICATED_DATA);
      byte[] ciphertext = cipher.doFinal(normalized.getBytes(StandardCharsets.US_ASCII));
      byte[] payload = ByteBuffer.allocate(iv.length + ciphertext.length)
          .put(iv)
          .put(ciphertext)
          .array();
      return VERSION_PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(payload);
    } catch (GeneralSecurityException exception) {
      throw new IllegalStateException("account number encryption failed", exception);
    }
  }

  private String hashNormalized(String normalized) {
    try {
      Mac mac = Mac.getInstance(HMAC_ALGORITHM);
      mac.init(hashKey);
      String contextualized = HASH_CONTEXT + normalized;
      return HexFormat.of().formatHex(
          mac.doFinal(contextualized.getBytes(StandardCharsets.US_ASCII))
      );
    } catch (GeneralSecurityException exception) {
      throw new IllegalStateException("account number hashing failed", exception);
    }
  }

  private String maskNormalized(String normalized) {
    return "****-****-" + normalized.substring(normalized.length() - 4);
  }

  private String normalize(String accountNumber) {
    if (accountNumber == null) {
      throw new IllegalArgumentException("account number must not be null");
    }
    String trimmed = accountNumber.strip();
    StringBuilder normalized = new StringBuilder(trimmed.length());
    for (int index = 0; index < trimmed.length(); index++) {
      char character = trimmed.charAt(index);
      if (character == '-') {
        continue;
      }
      if (character < '0' || character > '9') {
        throw new IllegalArgumentException("account number must contain only digits and hyphens");
      }
      normalized.append(character);
    }
    if (normalized.length() < MINIMUM_ACCOUNT_NUMBER_LENGTH
        || normalized.length() > MAXIMUM_ACCOUNT_NUMBER_LENGTH) {
      throw new IllegalArgumentException("account number length must be between 8 and 14 digits");
    }
    return normalized.toString();
  }

  private SecretKey encryptionKey(String encodedKey) {
    byte[] keyBytes = decodeKey(encodedKey, "encryption-key");
    try {
      if (keyBytes.length != AES_KEY_BYTES) {
        throw new IllegalArgumentException(
            "security.account-number.encryption-key must contain exactly 32 bytes"
        );
      }
      return new SecretKeySpec(keyBytes, "AES");
    } finally {
      Arrays.fill(keyBytes, (byte) 0);
    }
  }

  private SecretKey hashKey(String encodedKey) {
    byte[] keyBytes = decodeKey(encodedKey, "hash-key");
    try {
      if (keyBytes.length < MINIMUM_HMAC_KEY_BYTES) {
        throw new IllegalArgumentException(
            "security.account-number.hash-key must contain at least 32 bytes"
        );
      }
      return new SecretKeySpec(keyBytes, HMAC_ALGORITHM);
    } finally {
      Arrays.fill(keyBytes, (byte) 0);
    }
  }

  private byte[] decodeKey(String encodedKey, String propertyName) {
    try {
      return Base64.getDecoder().decode(encodedKey);
    } catch (IllegalArgumentException exception) {
      throw new IllegalArgumentException(
          "security.account-number." + propertyName + " must be valid Base64",
          exception
      );
    }
  }

  public record ProtectedAccountNumber(
      @JsonIgnore String encrypted,
      @JsonIgnore String hash,
      String masked
  ) {

    public ProtectedAccountNumber {
      Objects.requireNonNull(encrypted, "encrypted must not be null");
      Objects.requireNonNull(hash, "hash must not be null");
      Objects.requireNonNull(masked, "masked must not be null");
    }

    @Override
    public String toString() {
      return "ProtectedAccountNumber[masked=%s]".formatted(masked);
    }
  }
}
