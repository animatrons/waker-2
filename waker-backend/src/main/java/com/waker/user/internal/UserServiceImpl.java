package com.waker.user.internal;

import com.waker.common.AuthRateLimiter;
import com.waker.common.EmailAlreadyRegisteredException;
import com.waker.common.InvalidCredentialsException;
import com.waker.common.JwtProperties;
import com.waker.user.LoginRequest;
import com.waker.user.LoginResponse;
import com.waker.user.RegisterUserRequest;
import com.waker.user.UserResponse;
import com.waker.user.UserService;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class UserServiceImpl implements UserService {

  /**
   * Precomputed BCrypt hash used on unknown-email paths so {@code matches()} still runs and timing
   * is closer to the wrong-password path.
   */
  private static final String DUMMY_BCRYPT =
      "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtEncoder jwtEncoder;
  private final JwtProperties jwtProperties;
  private final AuthRateLimiter authRateLimiter;
  private final Clock clock;

  UserServiceImpl(
      UserRepository userRepository,
      PasswordEncoder passwordEncoder,
      JwtEncoder jwtEncoder,
      JwtProperties jwtProperties,
      AuthRateLimiter authRateLimiter,
      Clock clock) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
    this.jwtEncoder = jwtEncoder;
    this.jwtProperties = jwtProperties;
    this.authRateLimiter = authRateLimiter;
    this.clock = clock;
  }

  @Override
  @Transactional
  public UserResponse register(RegisterUserRequest request) {
    String email = request.email().trim().toLowerCase();
    authRateLimiter.checkOrThrow("email:" + email);

    Instant now = Instant.now(clock);
    String passwordHash = passwordEncoder.encode(request.password());

    User user =
        new User(
            UUID.randomUUID(),
            email,
            passwordHash,
            request.firstName().trim(),
            request.lastName().trim(),
            now,
            now);

    try {
      User saved = userRepository.saveAndFlush(user);
      return toResponse(saved);
    } catch (DataIntegrityViolationException ex) {
      throw new EmailAlreadyRegisteredException();
    }
  }

  @Override
  @Transactional(readOnly = true)
  public LoginResponse login(LoginRequest request) {
    String email = request.email().trim().toLowerCase();
    authRateLimiter.checkOrThrow("email:" + email);

    User user = userRepository.findByEmail(email).orElse(null);

    String passwordHash = user != null ? user.getPasswordHash() : DUMMY_BCRYPT;
    if (!passwordEncoder.matches(request.password(), passwordHash) || user == null) {
      throw new InvalidCredentialsException();
    }

    Instant now = Instant.now(clock);
    Instant expiresAt = now.plus(jwtProperties.expiration());
    JwtClaimsSet claims =
        JwtClaimsSet.builder()
            .subject(user.getId().toString())
            .claim("email", user.getEmail())
            .issuedAt(now)
            .expiresAt(expiresAt)
            .build();
    String accessToken =
        jwtEncoder
            .encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
            .getTokenValue();

    return new LoginResponse(accessToken, "Bearer", jwtProperties.expiration().toSeconds());
  }

  private static UserResponse toResponse(User user) {
    return new UserResponse(
        user.getId(),
        user.getEmail(),
        user.getFirstName(),
        user.getLastName(),
        user.getCreatedAt());
  }
}
