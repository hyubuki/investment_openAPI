package dev.hyuki.investment_openapi.support;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

public abstract class RedisBackedIntegrationTest {

  private static final int REDIS_PORT = 6379;

  protected static final GenericContainer<?> REDIS = new GenericContainer<>(
      DockerImageName.parse("redis:7.4-alpine")
  ).withExposedPorts(REDIS_PORT);

  static {
    REDIS.start();
  }

  @Autowired
  private StringRedisTemplate redisTemplate;

  @DynamicPropertySource
  static void redisProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.data.redis.host", REDIS::getHost);
    registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(REDIS_PORT));
  }

  @BeforeEach
  void clearRedis() {
    redisTemplate.execute((RedisCallback<Void>) connection -> {
      connection.serverCommands().flushDb();
      return null;
    });
  }
}
