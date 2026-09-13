package dev.hyuki.investment_openapi.user.repository;

import dev.hyuki.investment_openapi.user.entity.User;
import dev.hyuki.investment_openapi.user.entity.UserStatus;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, UUID> {

  Optional<User> findByEmail(String email);

  boolean existsByEmail(String email);

  Optional<User> findByUserIdAndStatus(UUID userId, UserStatus status);
}
