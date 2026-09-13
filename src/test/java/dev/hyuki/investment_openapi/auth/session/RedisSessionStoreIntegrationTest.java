package dev.hyuki.investment_openapi.auth.session;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
class RedisSessionStoreIntegrationTest {

  private static final int REDIS_PORT = 6379;
  private static final Instant NOW = Instant.parse("2026-09-13T01:00:00Z");

  @Container
  private static final GenericContainer<?> REDIS = new GenericContainer<>(
      DockerImageName.parse("redis:7.4-alpine")
  ).withExposedPorts(REDIS_PORT);

  private LettuceConnectionFactory connectionFactory;
  private StringRedisTemplate redisTemplate;
  private RedisSessionStore sessionStore;
  private RefreshTokenHasher refreshTokenHasher;

  @BeforeEach
  void setUp() {
    connectionFactory = new LettuceConnectionFactory(
        REDIS.getHost(),
        REDIS.getMappedPort(REDIS_PORT)
    );
    connectionFactory.afterPropertiesSet();
    redisTemplate = new StringRedisTemplate(connectionFactory);
    redisTemplate.afterPropertiesSet();
    redisTemplate.execute((RedisCallback<Void>) connection -> {
      connection.serverCommands().flushDb();
      return null;
    });
    sessionStore = new RedisSessionStore(
        redisTemplate,
        Clock.fixed(NOW, ZoneOffset.UTC)
    );
    refreshTokenHasher = new RefreshTokenHasher();
  }

  @AfterEach
  void tearDown() {
    connectionFactory.destroy();
  }

  @Test
  @DisplayName("Refresh Token 원문 대신 SHA-256 Hash를 Redis에 TTL과 함께 저장한다")
  void storesOnlyRefreshTokenHashWithTtl() {
    UUID userId = UUID.randomUUID();
    UUID sessionId = UUID.randomUUID();
    String refreshToken = "signed-refresh-token";
    String refreshTokenHash = refreshTokenHasher.hash(refreshToken);
    AuthSession session = session(
        userId,
        sessionId,
        refreshTokenHash,
        NOW.plusSeconds(60)
    );

    sessionStore.save(session);

    String rawValue = redisTemplate.opsForValue().get(RedisSessionStore.key(userId));
    Long ttl = redisTemplate.getExpire(RedisSessionStore.key(userId), TimeUnit.SECONDS);
    assertThat(rawValue)
        .contains(refreshTokenHash)
        .doesNotContain(refreshToken);
    assertThat(ttl).isPositive().isLessThanOrEqualTo(60);
    assertThat(sessionStore.matches(userId, sessionId, refreshTokenHash)).isTrue();
  }

  @Test
  @DisplayName("같은 사용자의 새 Session을 저장하면 이전 Session을 원자적으로 대체한다")
  void replacesPreviousSessionForSameUser() {
    UUID userId = UUID.randomUUID();
    AuthSession previous = session(
        userId,
        UUID.randomUUID(),
        refreshTokenHasher.hash("previous-refresh-token"),
        NOW.plusSeconds(60)
    );
    AuthSession current = session(
        userId,
        UUID.randomUUID(),
        refreshTokenHasher.hash("current-refresh-token"),
        NOW.plusSeconds(120)
    );

    sessionStore.save(previous);
    sessionStore.save(current);

    assertThat(sessionStore.findByUserId(userId)).contains(current);
    assertThat(sessionStore.matches(
        userId,
        previous.sessionId(),
        previous.refreshTokenHash()
    )).isFalse();
    assertThat(sessionStore.matches(
        userId,
        current.sessionId(),
        current.refreshTokenHash()
    )).isTrue();
  }

  @Test
  @DisplayName("Session TTL이 만료되면 Redis에서 자동 제거되어 활성 상태로 조회되지 않는다")
  void expiresSessionByRedisTtl() {
    UUID userId = UUID.randomUUID();
    sessionStore.save(session(
        userId,
        UUID.randomUUID(),
        refreshTokenHasher.hash("short-lived-refresh-token"),
        NOW.plusMillis(200)
    ));

    await().atMost(Duration.ofSeconds(2))
        .untilAsserted(() -> assertThat(sessionStore.findByUserId(userId)).isEmpty());
  }

  private AuthSession session(
      UUID userId,
      UUID sessionId,
      String refreshTokenHash,
      Instant expiresAt
  ) {
    return new AuthSession(userId, sessionId, refreshTokenHash, NOW, expiresAt);
  }
}
