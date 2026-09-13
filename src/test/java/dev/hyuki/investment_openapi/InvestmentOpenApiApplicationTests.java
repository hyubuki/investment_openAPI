package dev.hyuki.investment_openapi;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class InvestmentOpenApiApplicationTests {

  @Test
  @DisplayName("테스트 프로파일로 애플리케이션 컨텍스트를 정상적으로 로드한다")
  void contextLoads() {
  }
}
