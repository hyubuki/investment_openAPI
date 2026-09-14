package dev.hyuki.investment_openapi.auth.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.hyuki.investment_openapi.auth.session.RedisSessionStore;
import dev.hyuki.investment_openapi.auth.token.JwtTokenProvider;
import dev.hyuki.investment_openapi.support.RedisBackedIntegrationTest;
import dev.hyuki.investment_openapi.user.entity.User;
import dev.hyuki.investment_openapi.user.repository.UserRepository;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthApiIntegrationTest extends RedisBackedIntegrationTest {

  private static final String EMAIL = "trader@example.com";
  private static final String PASSWORD = "valid-password-123!";

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private JwtTokenProvider tokenProvider;

  @Autowired
  private RedisSessionStore sessionStore;

  @BeforeEach
  void clearUsers() {
    userRepository.deleteAll();
  }

  @Test
  @DisplayName("로그인 성공 시 기존 Session을 교체하고 사용자 로그인 시각을 기록한다")
  void loginReplacesExistingSessionAndRecordsLoginTime() throws Exception {
    JsonNode registration = register();
    String registrationRefreshToken = registration.path("tokens").path("refreshToken").asText();

    JsonNode login = responseBody(postJson("/api/v1/auth/login", Map.of(
        "email", "  TRADER@EXAMPLE.COM  ",
        "password", PASSWORD
    )).andExpect(status().isOk())
        .andExpect(jsonPath("$.tokenType").value("Bearer"))
        .andExpect(jsonPath("$.expiresIn").value(1800))
        .andExpect(jsonPath("$.refreshExpiresIn").value(86400)));

    String loginRefreshToken = login.path("refreshToken").asText();
    assertThat(loginRefreshToken).isNotEqualTo(registrationRefreshToken);
    assertThat(tokenProvider.readRefresh(loginRefreshToken).sessionId())
        .isNotEqualTo(tokenProvider.readRefresh(registrationRefreshToken).sessionId());
    User user = userRepository.findByEmail(EMAIL).orElseThrow();
    assertThat(user.getLastLoginAt()).isNotNull();
    assertThat(sessionStore.findByUserId(user.getUserId()))
        .get()
        .returns(
            tokenProvider.readRefresh(loginRefreshToken).sessionId(),
            session -> session.sessionId()
        );

    refresh(registrationRefreshToken)
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("SESSION_INVALID"));
  }

  @Test
  @DisplayName("미등록 이메일과 잘못된 비밀번호를 동일한 INVALID_CREDENTIALS로 응답한다")
  void rejectsUnknownEmailAndWrongPasswordWithoutAccountDisclosure() throws Exception {
    register();

    postJson("/api/v1/auth/login", Map.of(
        "email", "unknown@example.com",
        "password", PASSWORD
    )).andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
        .andExpect(jsonPath("$.detail").value("The supplied credentials are invalid."));

    postJson("/api/v1/auth/login", Map.of(
        "email", EMAIL,
        "password", "wrong-password"
    )).andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
        .andExpect(jsonPath("$.detail").value("The supplied credentials are invalid."));
  }

  @Test
  @DisplayName("Refresh Token을 회전하고 이전 Token 재사용 시 Session Family를 폐기한다")
  void rotatesRefreshTokenAndRevokesFamilyWhenPreviousTokenIsReused() throws Exception {
    String previousRefreshToken = register().path("tokens").path("refreshToken").asText();

    JsonNode rotated = responseBody(refresh(previousRefreshToken)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accessToken").isNotEmpty())
        .andExpect(jsonPath("$.refreshToken").isNotEmpty()));
    String currentRefreshToken = rotated.path("refreshToken").asText();
    assertThat(currentRefreshToken).isNotEqualTo(previousRefreshToken);

    refresh(previousRefreshToken)
        .andExpect(status().isUnauthorized())
        .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.code").value("REFRESH_TOKEN_REUSED"));

    refresh(currentRefreshToken)
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("SESSION_INVALID"));
  }

  @Test
  @DisplayName("로그아웃은 현재 Session을 폐기하고 같은 Access Token 재요청에도 204를 반환한다")
  void logoutRevokesCurrentSessionIdempotently() throws Exception {
    JsonNode registration = register();
    String accessToken = registration.path("tokens").path("accessToken").asText();
    String refreshToken = registration.path("tokens").path("refreshToken").asText();

    logout(accessToken).andExpect(status().isNoContent());
    logout(accessToken).andExpect(status().isNoContent());

    refresh(refreshToken)
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("SESSION_INVALID"));
  }

  @Test
  @DisplayName("로그아웃 요청에 Bearer Access Token이 없으면 UNAUTHORIZED를 반환한다")
  void rejectsLogoutWithoutBearerAccessToken() throws Exception {
    mockMvc.perform(post("/api/v1/auth/logout"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
  }

  @Test
  @DisplayName("유효한 Access Token과 활성 Session으로 현재 사용자 정보를 조회한다")
  void returnsCurrentUserForValidAccessTokenAndActiveSession() throws Exception {
    JsonNode registration = register();
    String accessToken = registration.path("tokens").path("accessToken").asText();

    mockMvc.perform(get("/api/v1/users/me")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.userId").value(registration.path("userId").asText()))
        .andExpect(jsonPath("$.email").value(EMAIL))
        .andExpect(jsonPath("$.role").value("USER"))
        .andExpect(jsonPath("$.status").value("ACTIVE"))
        .andExpect(jsonPath("$.createdAt").isNotEmpty());
  }

  @Test
  @DisplayName("보호 API에 Access Token이 없거나 Refresh Token을 사용하면 인증을 거부한다")
  void rejectsMissingAccessTokenAndRefreshTokenOnProtectedApi() throws Exception {
    JsonNode registration = register();
    String refreshToken = registration.path("tokens").path("refreshToken").asText();

    mockMvc.perform(get("/api/v1/users/me"))
        .andExpect(status().isUnauthorized())
        .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

    mockMvc.perform(get("/api/v1/users/me")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + refreshToken))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("INVALID_TOKEN"));
  }

  @Test
  @DisplayName("재로그인으로 교체된 Session의 이전 Access Token을 보호 API에서 거부한다")
  void rejectsAccessTokenFromReplacedSession() throws Exception {
    JsonNode registration = register();
    String previousAccessToken = registration.path("tokens").path("accessToken").asText();
    JsonNode login = responseBody(postJson("/api/v1/auth/login", Map.of(
        "email", EMAIL,
        "password", PASSWORD
    )).andExpect(status().isOk()));
    String currentAccessToken = login.path("accessToken").asText();

    mockMvc.perform(get("/api/v1/users/me")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + previousAccessToken))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("SESSION_INVALID"));

    mockMvc.perform(get("/api/v1/users/me")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + currentAccessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.email").value(EMAIL));
  }

  private JsonNode register() throws Exception {
    return responseBody(postJson("/api/v1/users", Map.of(
        "email", EMAIL,
        "password", PASSWORD
    )).andExpect(status().isCreated()));
  }

  private ResultActions refresh(String refreshToken) throws Exception {
    return postJson("/api/v1/auth/refresh", Map.of("refreshToken", refreshToken));
  }

  private ResultActions logout(String accessToken) throws Exception {
    return mockMvc.perform(post("/api/v1/auth/logout")
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken));
  }

  private ResultActions postJson(String path, Map<String, String> body) throws Exception {
    return mockMvc.perform(post(path)
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsBytes(body)));
  }

  private JsonNode responseBody(ResultActions result) throws Exception {
    return objectMapper.readTree(result.andReturn().getResponse().getContentAsString());
  }
}
