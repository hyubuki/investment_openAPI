package dev.hyuki.investment_openapi.auth.filter;

import dev.hyuki.investment_openapi.auth.service.AuthService;
import dev.hyuki.investment_openapi.support.error.ApiException;
import dev.hyuki.investment_openapi.user.entity.User;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private static final String POST = "POST";

  private final BearerTokenExtractor bearerTokenExtractor;
  private final AuthService authService;
  private final HandlerExceptionResolver exceptionResolver;

  public JwtAuthenticationFilter(
      BearerTokenExtractor bearerTokenExtractor,
      AuthService authService,
      HandlerExceptionResolver exceptionResolver
  ) {
    this.bearerTokenExtractor = bearerTokenExtractor;
    this.authService = authService;
    this.exceptionResolver = exceptionResolver;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request,
      HttpServletResponse response,
      FilterChain filterChain
  ) throws ServletException, IOException {
    String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
    if (authorization == null || SecurityContextHolder.getContext().getAuthentication() != null) {
      filterChain.doFilter(request, response);
      return;
    }

    try {
      User user = authService.authenticateAccessToken(bearerTokenExtractor.extract(authorization));
      var authentication = UsernamePasswordAuthenticationToken.authenticated(
          user,
          null,
          List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
      );
      authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
      SecurityContext context = SecurityContextHolder.createEmptyContext();
      context.setAuthentication(authentication);
      SecurityContextHolder.setContext(context);
      filterChain.doFilter(request, response);
    } catch (ApiException exception) {
      SecurityContextHolder.clearContext();
      exceptionResolver.resolveException(request, response, null, exception);
    }
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    if (!POST.equals(request.getMethod())) {
      return false;
    }
    String applicationPath = request.getRequestURI().substring(request.getContextPath().length());
    return switch (applicationPath) {
      case "/api/v1/users",
           "/api/v1/auth/login",
           "/api/v1/auth/refresh",
           "/api/v1/auth/logout" -> true;
      default -> false;
    };
  }
}
