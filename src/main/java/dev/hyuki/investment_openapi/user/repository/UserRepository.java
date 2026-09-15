package dev.hyuki.investment_openapi.user.repository;

import dev.hyuki.investment_openapi.user.entity.User;
import dev.hyuki.investment_openapi.user.entity.UserStatus;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, UUID> {

  Optional<User> findByEmail(String email);

  boolean existsByEmail(String email);

  Optional<User> findByUserIdAndStatus(UUID userId, UserStatus status);

  @Modifying
  @Query(value = """
      UPDATE users
         SET failed_login_count = CASE
                 WHEN status = 'LOCKED' THEN 1
                 ELSE failed_login_count + 1
             END,
             status = CASE
                 WHEN (CASE
                     WHEN status = 'LOCKED' THEN 1
                     ELSE failed_login_count + 1
                 END) >= :maxFailedAttempts THEN 'LOCKED'
                 ELSE 'ACTIVE'
             END,
             locked_until = CASE
                 WHEN (CASE
                     WHEN status = 'LOCKED' THEN 1
                     ELSE failed_login_count + 1
                 END) >= :maxFailedAttempts THEN :lockedUntil
                 ELSE NULL
             END,
             updated_at = :failedAt
       WHERE email = :email
         AND (
             status = 'ACTIVE'
             OR (status = 'LOCKED' AND locked_until IS NOT NULL AND locked_until <= :failedAt)
         )
      """, nativeQuery = true)
  int recordLoginFailure(
      @Param("email") String email,
      @Param("maxFailedAttempts") int maxFailedAttempts,
      @Param("lockedUntil") Instant lockedUntil,
      @Param("failedAt") Instant failedAt
  );
}
