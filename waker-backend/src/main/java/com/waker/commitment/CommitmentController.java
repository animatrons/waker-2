package com.waker.commitment;

import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/commitments")
public class CommitmentController {

  private final CommitmentService commitmentService;

  public CommitmentController(CommitmentService commitmentService) {
    this.commitmentService = commitmentService;
  }

  @PostMapping
  public ResponseEntity<CommitmentResponse> create(
      @AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CreateCommitmentRequest request) {
    UUID ownerId = UUID.fromString(jwt.getSubject());
    CommitmentResponse created = commitmentService.create(ownerId, request);
    return ResponseEntity.status(HttpStatus.CREATED).body(created);
  }
}
