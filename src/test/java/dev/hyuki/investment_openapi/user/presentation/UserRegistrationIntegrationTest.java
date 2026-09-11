package dev.hyuki.investment_openapi.user.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.hyuki.investment_openapi.user.entity.User;
import dev.hyuki.investment_openapi.user.entity.UserRole;
import dev.hyuki.investment_openapi.user.entity.UserStatus;
import dev.hyuki.investment_openapi.user.repository.UserRepository;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserRegistrationIntegrationTest {

  private static final String PASSWORD = "valid-password-123!";

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private PasswordEncoder passwordEncoder;

  @BeforeEach
  void clearUsers() {
    userRepository.deleteAll();
  }

  @Test
  void registersNormalizedEmailWithServerControlledRoleAndPasswordHash() throws Exception {
    String requestId = "registration-request-001";

    mockMvc.perform(post("/api/v1/users")
            .header("X-Request-Id", requestId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsBytes(Map.of(
                "email", "  TRADER@EXAMPLE.COM  ",
                "password", PASSWORD,
                "role", "ADMIN"
            ))))
        .andExpect(status().isCreated())
        .andExpect(header().string("X-Request-Id", requestId))
        .andExpect(header().string("Location", org.hamcrest.Matchers.matchesPattern(
            ".*/api/v1/users/[0-9a-f-]+$"
        )))
        .andExpect(jsonPath("$.email").value("trader@example.com"))
        .andExpect(jsonPath("$.role").value("USER"))
        .andExpect(jsonPath("$.status").value("ACTIVE"))
        .andExpect(jsonPath("$.password").doesNotExist())
        .andExpect(jsonPath("$.passwordHash").doesNotExist());

    User saved = userRepository.findByEmail("trader@example.com").orElseThrow();
    assertThat(saved.getRole()).isEqualTo(UserRole.USER);
    assertThat(saved.getStatus()).isEqualTo(UserStatus.ACTIVE);
    assertThat(saved.getPasswordHash()).isNotEqualTo(PASSWORD);
    assertThat(passwordEncoder.matches(PASSWORD, saved.getPasswordHash())).isTrue();
  }

  @Test
  void rejectsNormalizedDuplicateEmail() throws Exception {
    register("Trader@Example.com", PASSWORD).andExpect(status().isCreated());

    register(" trader@example.com ", PASSWORD)
        .andExpect(status().isConflict())
        .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.code").value("EMAIL_ALREADY_EXISTS"))
        .andExpect(jsonPath("$.retryable").value(false));

    assertThat(userRepository.count()).isOne();
  }

  @Test
  void validatesEmailAndUtf8PasswordByteLength() throws Exception {
    register("not-an-email", PASSWORD)
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    register("short@example.com", "short")
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.violations[0].field").value("password"));
    register("too-long@example.com", "가".repeat(25))
        .andExpect(status().isBadRequest());
    register("boundary@example.com", "가".repeat(24))
        .andExpect(status().isCreated());
  }

  private org.springframework.test.web.servlet.ResultActions register(
      String email,
      String password
  ) throws Exception {
    return mockMvc.perform(post("/api/v1/users")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsBytes(Map.of(
            "email", email,
            "password", password
        ))));
  }
}
