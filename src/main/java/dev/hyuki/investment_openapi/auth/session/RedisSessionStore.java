package dev.hyuki.investment_openapi.auth.session;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Repository;

@Repository
public class RedisSessionStore {

  private static final String KEY_PREFIX = "auth:session:user:";
  private static final String FIELD_SEPARATOR = "\\|";
  private static final DefaultRedisScript<Long> ROTATE_SCRIPT = new DefaultRedisScript<>("""
      local current = redis.call('GET', KEYS[1])
      if not current then
        return 0
      end

      local _, sessionId, refreshHash = string.match(current, '^([^|]+)|([^|]+)|([^|]+)|')
      if not sessionId or sessionId ~= ARGV[1] then
        return 0
      end
      if refreshHash ~= ARGV[2] then
        redis.call('DEL', KEYS[1])
        return -1
      end

      redis.call('SET', KEYS[1], ARGV[3], 'PX', ARGV[4])
      return 1
      """, Long.class);
  private static final DefaultRedisScript<Long> DELETE_IF_SESSION_MATCHES_SCRIPT =
      new DefaultRedisScript<>("""
          local current = redis.call('GET', KEYS[1])
          if not current then
            return 0
          end

          local _, sessionId = string.match(current, '^([^|]+)|([^|]+)|')
          if sessionId ~= ARGV[1] then
            return 0
          end

          return redis.call('DEL', KEYS[1])
          """, Long.class);

  private final StringRedisTemplate redisTemplate;
  private final Clock clock;

  public RedisSessionStore(StringRedisTemplate redisTemplate, Clock clock) {
    this.redisTemplate = redisTemplate;
    this.clock = clock;
  }

  public void save(AuthSession session) {
    Duration ttl = ttl(session);
    redisTemplate.opsForValue().set(key(session.userId()), encode(session), ttl);
  }

  public Optional<AuthSession> findByUserId(UUID userId) {
    String value = redisTemplate.opsForValue().get(key(userId));
    if (value == null) {
      return Optional.empty();
    }
    AuthSession session = decode(value);
    if (!session.userId().equals(userId)) {
      throw new IllegalStateException("Redis session user does not match its key");
    }
    return Optional.of(session);
  }

  public boolean matches(UUID userId, UUID sessionId, String refreshTokenHash) {
    return findByUserId(userId)
        .filter(session -> session.sessionId().equals(sessionId))
        .map(session -> MessageDigest.isEqual(
            session.refreshTokenHash().getBytes(StandardCharsets.US_ASCII),
            refreshTokenHash.getBytes(StandardCharsets.US_ASCII)
        ))
        .orElse(false);
  }

  public boolean hasActiveSession(UUID userId, UUID sessionId) {
    return findByUserId(userId)
        .map(session -> session.sessionId().equals(sessionId))
        .orElse(false);
  }

  public SessionRotationResult rotate(
      AuthSession replacement,
      String expectedRefreshTokenHash
  ) {
    Duration ttl = ttl(replacement);
    Long result = redisTemplate.execute(
        ROTATE_SCRIPT,
        List.of(key(replacement.userId())),
        replacement.sessionId().toString(),
        expectedRefreshTokenHash,
        encode(replacement),
        Long.toString(ttl.toMillis())
    );
    if (result == null) {
      throw new IllegalStateException("Redis did not return a session rotation result");
    }
    if (result == 1L) {
      return SessionRotationResult.ROTATED;
    }
    if (result == -1L) {
      return SessionRotationResult.REUSED;
    }
    return SessionRotationResult.INVALID;
  }

  public boolean deleteIfSessionMatches(UUID userId, UUID sessionId) {
    Long deleted = redisTemplate.execute(
        DELETE_IF_SESSION_MATCHES_SCRIPT,
        List.of(key(userId)),
        sessionId.toString()
    );
    return deleted != null && deleted == 1L;
  }

  public void delete(UUID userId) {
    redisTemplate.delete(key(userId));
  }

  static String key(UUID userId) {
    return KEY_PREFIX + userId;
  }

  private String encode(AuthSession session) {
    return String.join(
        "|",
        session.userId().toString(),
        session.sessionId().toString(),
        session.refreshTokenHash(),
        session.issuedAt().toString(),
        session.expiresAt().toString()
    );
  }

  private Duration ttl(AuthSession session) {
    Duration ttl = Duration.between(clock.instant(), session.expiresAt());
    if (ttl.isZero() || ttl.isNegative()) {
      throw new IllegalArgumentException("session must expire in the future");
    }
    return ttl;
  }

  private AuthSession decode(String value) {
    String[] fields = value.split(FIELD_SEPARATOR, -1);
    if (fields.length != 5) {
      throw new IllegalStateException("Redis session data is malformed");
    }
    try {
      return new AuthSession(
          UUID.fromString(fields[0]),
          UUID.fromString(fields[1]),
          fields[2],
          Instant.parse(fields[3]),
          Instant.parse(fields[4])
      );
    } catch (RuntimeException exception) {
      throw new IllegalStateException("Redis session data is malformed", exception);
    }
  }
}
