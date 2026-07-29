package com.waker.common;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

/**
 * IP-dimension rate limit for {@code POST /api/v1/auth/login} and {@code POST
 * /api/v1/auth/register}. Account (email) dimension is enforced in {@code UserServiceImpl}.
 *
 * <p>Uses {@link HttpServletRequest#getRemoteAddr()} only — do not trust {@code X-Forwarded-For}
 * until a reverse proxy is in place.
 */
@Component
public class AuthRateLimitFilter extends OncePerRequestFilter {

  private final AuthRateLimiter authRateLimiter;
  private final ObjectMapper objectMapper;

  public AuthRateLimitFilter(AuthRateLimiter authRateLimiter, ObjectMapper objectMapper) {
    this.authRateLimiter = authRateLimiter;
    this.objectMapper = objectMapper;
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    if (!HttpMethod.POST.matches(request.getMethod())) {
      return true;
    }
    String path = request.getRequestURI();
    return !"/api/v1/auth/login".equals(path) && !"/api/v1/auth/register".equals(path);
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    try {
      authRateLimiter.checkOrThrow("ip:" + request.getRemoteAddr());
      filterChain.doFilter(request, response);
    } catch (RateLimitExceededException ex) {
      writeTooManyRequests(response, ex);
    }
  }

  private void writeTooManyRequests(HttpServletResponse response, RateLimitExceededException ex)
      throws IOException {
    response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
    response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
    if (ex.getRetryAfterSeconds() != null) {
      response.setHeader("Retry-After", String.valueOf(ex.getRetryAfterSeconds()));
    }
    ProblemDetail problem =
        ProblemDetail.forStatusAndDetail(HttpStatus.TOO_MANY_REQUESTS, ex.getMessage());
    problem.setTitle("Too Many Requests");
    problem.setType(URI.create("about:blank"));
    objectMapper.writeValue(response.getOutputStream(), problem);
  }
}
