package com.waker.common;

import java.net.URI;
import java.util.List;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import tools.jackson.databind.ObjectMapper;

/**
 * Single deny-by-default {@link SecurityFilterChain} for the modular monolith (AD-9). Public routes
 * are explicit {@code permitAll()} entries only. CSRF is disabled because the API is token-based
 * (JWT), not cookie-session; revisit if cookie sessions appear.
 */
@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(CorsProperties.class)
public class SecurityConfig {

  @Bean
  SecurityFilterChain securityFilterChain(
      HttpSecurity http,
      CorsConfigurationSource corsConfigurationSource,
      AuthenticationEntryPoint authenticationEntryPoint,
      AccessDeniedHandler accessDeniedHandler)
      throws Exception {
    http.csrf(csrf -> csrf.disable())
        .cors(cors -> cors.configurationSource(corsConfigurationSource))
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .httpBasic(httpBasic -> httpBasic.disable())
        .formLogin(formLogin -> formLogin.disable())
        .anonymous(anonymous -> anonymous.disable())
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers("/actuator/health", "/actuator/health/**")
                    .permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/v1/auth/register")
                    .permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/v1/auth/login")
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .oauth2ResourceServer(
            oauth2 ->
                oauth2
                    .jwt(Customizer.withDefaults())
                    .authenticationEntryPoint(authenticationEntryPoint)
                    .accessDeniedHandler(accessDeniedHandler))
        .exceptionHandling(
            exceptions ->
                exceptions
                    .authenticationEntryPoint(authenticationEntryPoint)
                    .accessDeniedHandler(accessDeniedHandler));

    return http.build();
  }

  @Bean
  PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  CorsConfigurationSource corsConfigurationSource(CorsProperties corsProperties) {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOrigins(List.copyOf(corsProperties.allowedOrigins()));
    configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    configuration.setAllowedHeaders(List.of("*"));
    configuration.setAllowCredentials(true);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }

  @Bean
  AuthenticationEntryPoint authenticationEntryPoint(ObjectMapper objectMapper) {
    return (request, response, authException) -> {
      response.setStatus(HttpStatus.UNAUTHORIZED.value());
      response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
      ProblemDetail problem =
          ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Authentication required");
      problem.setTitle("Unauthorized");
      problem.setType(URI.create("about:blank"));
      objectMapper.writeValue(response.getOutputStream(), problem);
    };
  }

  @Bean
  AccessDeniedHandler accessDeniedHandler(ObjectMapper objectMapper) {
    return (request, response, accessDeniedException) -> {
      response.setStatus(HttpStatus.FORBIDDEN.value());
      response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
      ProblemDetail problem =
          ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, "Access denied");
      problem.setTitle("Forbidden");
      problem.setType(URI.create("about:blank"));
      objectMapper.writeValue(response.getOutputStream(), problem);
    };
  }
}
