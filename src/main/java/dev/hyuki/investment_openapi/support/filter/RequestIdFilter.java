package dev.hyuki.investment_openapi.support.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestIdFilter extends OncePerRequestFilter {

  public static final String REQUEST_ID_ATTRIBUTE = RequestIdFilter.class.getName() + ".requestId";
  private static final String REQUEST_ID_HEADER = "X-Request-Id";
  private static final Pattern VALID_REQUEST_ID = Pattern.compile("[A-Za-z0-9._:-]{1,128}");

  @Override
  protected void doFilterInternal(
      HttpServletRequest request,
      HttpServletResponse response,
      FilterChain filterChain
  ) throws ServletException, IOException {
    String requestId = resolveRequestId(request.getHeader(REQUEST_ID_HEADER));
    request.setAttribute(REQUEST_ID_ATTRIBUTE, requestId);
    response.setHeader(REQUEST_ID_HEADER, requestId);
    filterChain.doFilter(request, response);
  }

  private String resolveRequestId(String suppliedRequestId) {
    if (suppliedRequestId != null && VALID_REQUEST_ID.matcher(suppliedRequestId).matches()) {
      return suppliedRequestId;
    }
    return UUID.randomUUID().toString();
  }
}
