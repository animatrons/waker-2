package com.waker.commitment;

import jakarta.validation.Valid;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/commitments")
public class CommitmentController {

  private final CommitmentService commitmentService;
  private final CommitmentProperties commitmentProperties;

  public CommitmentController(
      CommitmentService commitmentService, CommitmentProperties commitmentProperties) {
    this.commitmentService = commitmentService;
    this.commitmentProperties = commitmentProperties;
  }

  @GetMapping("/{id}")
  public ResponseEntity<CommitmentResponse> getById(
      @AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
    UUID ownerId = UUID.fromString(jwt.getSubject());
    return ResponseEntity.ok(commitmentService.getById(ownerId, id));
  }

  @GetMapping
  public ResponseEntity<CommitmentPageResponse> list(
      @AuthenticationPrincipal Jwt jwt,
      @RequestParam(required = false) CommitmentStatus status,
      @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
          Pageable pageable) {
    UUID ownerId = UUID.fromString(jwt.getSubject());
    validatePageSize(pageable);
    CommitmentPageResponse page =
        commitmentService.list(ownerId, Optional.ofNullable(status), pageable);
    return ResponseEntity.ok(page);
  }

  @PostMapping
  public ResponseEntity<CommitmentResponse> create(
      @AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CreateCommitmentRequest request) {
    UUID ownerId = UUID.fromString(jwt.getSubject());
    CommitmentResponse created = commitmentService.create(ownerId, request);
    return ResponseEntity.status(HttpStatus.CREATED).body(created);
  }

  @PutMapping("/{id}")
  public ResponseEntity<CommitmentResponse> update(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable UUID id,
      @Valid @RequestBody UpdateCommitmentRequest request) {
    UUID ownerId = UUID.fromString(jwt.getSubject());
    CommitmentResponse updated = commitmentService.update(ownerId, id, request);
    return ResponseEntity.ok(updated);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> cancel(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
    UUID ownerId = UUID.fromString(jwt.getSubject());
    commitmentService.cancel(ownerId, id);
    return ResponseEntity.noContent().build();
  }

  private void validatePageSize(Pageable pageable) {
    if (pageable.getPageSize() > commitmentProperties.maxPageSize()) {
      throw new CommitmentValidationException(
          "size must not exceed " + commitmentProperties.maxPageSize());
    }
  }
}
