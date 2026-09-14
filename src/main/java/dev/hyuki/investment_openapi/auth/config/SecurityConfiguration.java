package dev.hyuki.investment_openapi.auth.config;

import dev.hyuki.investment_openapi.auth.filter.BearerTokenExtractor;
import dev.hyuki.investment_openapi.auth.filter.JwtAuthenticationFilter;
import dev.hyuki.investment_openapi.auth.service.AuthService;
import dev.hyuki.investment_openapi.support.error.ApiException;
import dev.hyuki.investment_openapi.support.error.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

@Configuration
@EnableWebSecurity
public class SecurityConfiguration {

  @Bean
  public SecurityFilterChain securityFilterChain(
      HttpSecurity http,
      BearerTokenExtractor bearerTokenExtractor,
      AuthService authService,
      @Qualifier("handlerExceptionResolver") HandlerExceptionResolver exceptionResolver
  ) throws Exception {
    JwtAuthenticationFilter jwtAuthenticationFilter = new JwtAuthenticationFilter(
        bearerTokenExtractor,
        authService,
        exceptionResolver
    );
    http
        .csrf(csrf -> csrf.disable())
        .cors(cors -> cors.disable())
        .httpBasic(httpBasic -> httpBasic.disable())
        .formLogin(formLogin -> formLogin.disable())
        .logout(logout -> logout.disable())
        .sessionManagement(session -> session.sessionCreationPolicy(
            SessionCreationPolicy.STATELESS
        ))
        .authorizeHttpRequests(authorize -> authorize
            .requestMatchers(HttpMethod.POST, "/api/v1/users").permitAll()
            .requestMatchers(
                HttpMethod.POST,
                "/api/v1/auth/login",
                "/api/v1/auth/refresh",
                "/api/v1/auth/logout"
            ).permitAll()
            .requestMatchers("/error").permitAll()
            .anyRequest().authenticated()
        )
        .exceptionHandling(exceptions -> exceptions
            .authenticationEntryPoint((request, response, exception) -> resolve(
                exceptionResolver,
                request,
                response,
                new ApiException(
                    ErrorCode.UNAUTHORIZED,
                    "A valid access token is required.",
                    exception
                )
            ))
            .accessDeniedHandler((request, response, exception) -> resolve(
                exceptionResolver,
                request,
                response,
                new ApiException(
                    ErrorCode.FORBIDDEN,
                    "The authenticated user is not allowed to access this resource.",
                    exception
                )
            ))
        )
        .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

    return http.build();
  }

  private static void resolve(
      HandlerExceptionResolver exceptionResolver,
      HttpServletRequest request,
      HttpServletResponse response,
      ApiException exception
  ) {
    exceptionResolver.resolveException(request, response, null, exception);
  }
}
