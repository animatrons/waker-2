package com.waker.user.internal;

import com.waker.common.EmailAlreadyRegisteredException;
import com.waker.user.RegisterUserRequest;
import com.waker.user.UserResponse;
import com.waker.user.UserService;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class UserServiceImpl implements UserService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final Clock clock;

  UserServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder, Clock clock) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
    this.clock = clock;
  }

  @Override
  @Transactional
  public UserResponse register(RegisterUserRequest request) {
    Instant now = Instant.now(clock);
    String passwordHash = passwordEncoder.encode(request.password());

    User user =
        new User(
            UUID.randomUUID(),
            request.email().trim().toLowerCase(),
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

  private static UserResponse toResponse(User user) {
    return new UserResponse(
        user.getId(),
        user.getEmail(),
        user.getFirstName(),
        user.getLastName(),
        user.getCreatedAt());
  }
}
