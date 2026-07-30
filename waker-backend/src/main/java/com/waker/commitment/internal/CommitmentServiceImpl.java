package com.waker.commitment.internal;

import com.waker.commitment.CommitmentProperties;
import com.waker.commitment.CommitmentResponse;
import com.waker.commitment.CommitmentService;
import com.waker.commitment.CommitmentStatus;
import com.waker.commitment.CommitmentValidationException;
import com.waker.commitment.ConcurrentCommitmentCapExceededException;
import com.waker.commitment.CreateCommitmentRequest;
import com.waker.mission.MissionConfig;
import com.waker.mission.MissionDispatch;
import com.waker.penalty.InvalidPenaltyConfigException;
import com.waker.penalty.PenaltyConfig;
import com.waker.penalty.PenaltyDispatch;
import com.waker.user.UserService;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class CommitmentServiceImpl implements CommitmentService {

  private final UserService userService;
  private final CommitmentRepository commitmentRepository;
  private final MissionDispatch missionDispatch;
  private final PenaltyDispatch penaltyDispatch;
  private final CommitmentCreateValidator createValidator;
  private final CommitmentMapper commitmentMapper;
  private final CommitmentProperties commitmentProperties;
  private final Clock clock;

  CommitmentServiceImpl(
      UserService userService,
      CommitmentRepository commitmentRepository,
      MissionDispatch missionDispatch,
      PenaltyDispatch penaltyDispatch,
      CommitmentCreateValidator createValidator,
      CommitmentMapper commitmentMapper,
      CommitmentProperties commitmentProperties,
      Clock clock) {
    this.userService = userService;
    this.commitmentRepository = commitmentRepository;
    this.missionDispatch = missionDispatch;
    this.penaltyDispatch = penaltyDispatch;
    this.createValidator = createValidator;
    this.commitmentMapper = commitmentMapper;
    this.commitmentProperties = commitmentProperties;
    this.clock = clock;
  }

  @Override
  @Transactional
  public CommitmentResponse create(UUID ownerId, CreateCommitmentRequest request) {
    userService.lockById(ownerId);

    long pending = commitmentRepository.countByUserIdAndStatus(ownerId, CommitmentStatus.PENDING);
    if (pending >= commitmentProperties.maxPending()) {
      throw new ConcurrentCommitmentCapExceededException();
    }

    Instant now = Instant.now(clock);
    Instant notifyTime = request.notifyTime().toInstant();
    Instant deadline = request.deadline().toInstant();
    createValidator.validateTiming(now, notifyTime, deadline);

    MissionConfig missionConfig = validateAndPrepareMission(request.missionConfig());
    PenaltyConfig penaltyConfig = validatePenalty(request.penaltyConfig());

    Commitment commitment =
        new Commitment(
            UUID.randomUUID(),
            ownerId,
            request.name().trim(),
            normalizeDescription(request.description()),
            CommitmentStatus.PENDING,
            notifyTime,
            deadline,
            missionConfig,
            penaltyConfig,
            now,
            now);

    Commitment saved = commitmentRepository.saveAndFlush(commitment);
    return commitmentMapper.toResponse(saved);
  }

  private MissionConfig validateAndPrepareMission(MissionConfig config) {
    try {
      missionDispatch.validateConfig(config);
      return missionDispatch.prepareForPersist(config);
    } catch (IllegalArgumentException ex) {
      throw new CommitmentValidationException(ex.getMessage());
    }
  }

  private PenaltyConfig validatePenalty(PenaltyConfig config) {
    try {
      penaltyDispatch.validateConfig(config);
      return config;
    } catch (InvalidPenaltyConfigException ex) {
      throw new CommitmentValidationException(ex.getMessage());
    }
  }

  private static String normalizeDescription(String description) {
    if (description == null) {
      return null;
    }
    String trimmed = description.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }
}
