package dev.hyuki.investment_openapi.user.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import dev.hyuki.investment_openapi.user.support.EmailNormalizer;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "users")
public class User {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "user_id", nullable = false, updatable = false)
  private UUID userId;

  @Column(name = "email", nullable = false, length = 100)
  private String email;

  @Column(name = "password_hash", nullable = false, length = 100)
  private String passwordHash;

  @Enumerated(EnumType.STRING)
  @Column(name = "role", nullable = false, length = 32)
  private UserRole role;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 32)
  private UserStatus status;

  @Column(name = "failed_login_count", nullable = false)
  private int failedLoginCount;

  @Column(name = "locked_until")
  private Instant lockedUntil;

  @Column(name = "last_login_at")
  private Instant lastLoginAt;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected User() {
  }

  private User(String email, String passwordHash, Instant registeredAt) {
    this.email = EmailNormalizer.normalize(email);
    this.passwordHash = Objects.requireNonNull(passwordHash);
    this.role = UserRole.USER;
    this.status = UserStatus.ACTIVE;
    this.failedLoginCount = 0;
    this.createdAt = Objects.requireNonNull(registeredAt);
    this.updatedAt = registeredAt;
  }

  public static User register(String email, String passwordHash, Instant registeredAt) {
    return new User(email, passwordHash, registeredAt);
  }

  public void recordSuccessfulLogin(Instant loggedInAt) {
    Instant now = Objects.requireNonNull(loggedInAt, "loggedInAt must not be null");
    this.failedLoginCount = 0;
    this.lockedUntil = null;
    this.lastLoginAt = now;
    this.updatedAt = now;
  }

  public void releaseExpiredLoginLock(Instant attemptedAt) {
    Instant now = Objects.requireNonNull(attemptedAt, "attemptedAt must not be null");
    if (status == UserStatus.LOCKED && lockedUntil != null && !lockedUntil.isAfter(now)) {
      this.status = UserStatus.ACTIVE;
      this.failedLoginCount = 0;
      this.lockedUntil = null;
      this.updatedAt = now;
    }
  }

  public UUID getUserId() {
    return userId;
  }

  public String getEmail() {
    return email;
  }

  @JsonIgnore
  public String getPasswordHash() {
    return passwordHash;
  }

  public UserRole getRole() {
    return role;
  }

  public UserStatus getStatus() {
    return status;
  }

  public int getFailedLoginCount() {
    return failedLoginCount;
  }

  public Instant getLockedUntil() {
    return lockedUntil;
  }

  public Instant getLastLoginAt() {
    return lastLoginAt;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }
}
