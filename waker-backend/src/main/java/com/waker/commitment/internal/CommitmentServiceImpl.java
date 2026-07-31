package com.waker.commitment.internal;

import com.waker.commitment.CommitmentNotFoundException;
import com.waker.commitment.CommitmentPageResponse;
import com.waker.commitment.CommitmentProperties;
import com.waker.commitment.CommitmentResponse;
import com.waker.commitment.CommitmentService;
import com.waker.commitment.CommitmentStatus;
import com.waker.commitment.CommitmentValidationException;
import com.waker.commitment.ConcurrentCommitmentCapExceededException;
import com.waker.commitment.CreateCommitmentRequest;
import com.waker.commitment.EditWindowClosedException;
import com.waker.commitment.FulfillmentRejectedException;
import com.waker.commitment.InvalidCommitmentStateException;
import com.waker.commitment.UpdateCommitmentRequest;
import com.waker.mission.MissionConfig;
import com.waker.mission.MissionDispatch;
import com.waker.mission.MissionFulfillmentProof;
import com.waker.mission.MissionVerificationResult;
import com.waker.penalty.InvalidPenaltyConfigException;
import com.waker.penalty.PenaltyConfig;
import com.waker.penalty.PenaltyDispatch;
import com.waker.user.UserService;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
      @Qualifier("commitmentClock") Clock clock) {
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

  @Override
  @Transactional(readOnly = true)
  public CommitmentResponse getById(UUID ownerId, UUID id) {
    return commitmentRepository
        .findByIdAndUserId(id, ownerId)
        .map(commitmentMapper::toResponse)
        .orElseThrow(CommitmentNotFoundException::new);
  }

  @Override
  @Transactional(readOnly = true)
  public CommitmentPageResponse list(
      UUID ownerId, Optional<CommitmentStatus> status, Pageable pageable) {
    Page<Commitment> page =
        status
            .map(s -> commitmentRepository.findByUserIdAndStatus(ownerId, s, pageable))
            .orElseGet(() -> commitmentRepository.findByUserId(ownerId, pageable));

    List<CommitmentResponse> content =
        page.getContent().stream().map(commitmentMapper::toResponse).toList();

    return new CommitmentPageResponse(
        content, page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
  }

  @Override
  @Transactional
  public CommitmentResponse update(UUID ownerId, UUID id, UpdateCommitmentRequest request) {
    Commitment existing = loadPendingWithinEditWindow(ownerId, id);

    Instant notifyTime = request.notifyTime().toInstant();
    Instant deadline = request.deadline().toInstant();
    createValidator.validateTimingForEdit(existing.getCreatedAt(), notifyTime, deadline);

    MissionConfig missionConfig = validateAndPrepareMission(request.missionConfig());
    PenaltyConfig penaltyConfig = validatePenalty(request.penaltyConfig());

    Instant now = Instant.now(clock);
    int rows =
        commitmentRepository.updateIfPending(
            id,
            ownerId,
            request.name().trim(),
            normalizeDescription(request.description()),
            notifyTime,
            deadline,
            missionConfig,
            penaltyConfig,
            now);

    if (rows != 1) {
      throw new InvalidCommitmentStateException();
    }

    return new CommitmentResponse(
        id,
        request.name().trim(),
        normalizeDescription(request.description()),
        CommitmentStatus.PENDING,
        notifyTime,
        deadline,
        missionConfig,
        penaltyConfig,
        existing.getCreatedAt(),
        now);
  }

  @Override
  @Transactional
  public void cancel(UUID ownerId, UUID id) {
    loadPendingWithinEditWindow(ownerId, id);

    Instant now = Instant.now(clock);
    int rows = commitmentRepository.cancelIfPending(id, ownerId, now);
    if (rows != 1) {
      throw new InvalidCommitmentStateException();
    }
  }

  @Override
  @Transactional
  public CommitmentResponse fulfill(UUID ownerId, UUID id, MissionFulfillmentProof proof) {
    Commitment commitment =
        commitmentRepository
            .findByIdAndUserId(id, ownerId)
            .orElseThrow(CommitmentNotFoundException::new);

    if (commitment.getStatus() != CommitmentStatus.PENDING) {
      throw new InvalidCommitmentStateException();
    }

    MissionVerificationResult result;
    try {
      result = missionDispatch.verifyFulfillment(id, commitment.getMissionConfig(), proof);
    } catch (IllegalArgumentException ex) {
      throw new CommitmentValidationException(ex.getMessage());
    }

    if (!result.accepted()) {
      throw new FulfillmentRejectedException(result.rejectionReason());
    }

    Instant now = Instant.now(clock);
    int rows = commitmentRepository.fulfillIfPending(id, ownerId, now);
    if (rows != 1) {
      throw new InvalidCommitmentStateException();
    }

    return commitmentRepository
        .findByIdAndUserId(id, ownerId)
        .map(commitmentMapper::toResponse)
        .orElseThrow(CommitmentNotFoundException::new);
  }

  private Commitment loadPendingWithinEditWindow(UUID ownerId, UUID id) {
    Commitment commitment =
        commitmentRepository
            .findByIdAndUserId(id, ownerId)
            .orElseThrow(CommitmentNotFoundException::new);

    if (commitment.getStatus() != CommitmentStatus.PENDING) {
      throw new InvalidCommitmentStateException();
    }

    Instant now = Instant.now(clock);
    if (!now.isBefore(
        createValidator.editWindowEnd(commitment.getCreatedAt(), commitment.getNotifyTime()))) {
      throw new EditWindowClosedException();
    }

    return commitment;
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
