package com.waker.common;

import com.waker.commitment.CommitmentNotFoundException;
import com.waker.commitment.CommitmentValidationException;
import com.waker.commitment.ConcurrentCommitmentCapExceededException;
import com.waker.commitment.EditWindowClosedException;
import com.waker.commitment.FulfillmentRejectedException;
import com.waker.commitment.InvalidCommitmentStateException;
import com.waker.penalty.InvalidPenaltyConfigException;
import com.waker.user.UserNotFoundException;
import jakarta.validation.ConstraintViolationException;
import java.net.URI;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Shared RFC 7807 error contract for all controllers (AD-8). Controllers return {@link
 * ResponseEntity}; rejections surface as {@link ProblemDetail} — never a legacy ResponseDTO
 * envelope.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ProblemDetail> handleHttpMessageNotReadable(
      HttpMessageNotReadableException ex) {
    String detail =
        ex.getMostSpecificCause().getMessage() != null
            ? ex.getMostSpecificCause().getMessage()
            : "Malformed JSON request";
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
    problem.setTitle("Validation Failed");
    problem.setType(URI.create("about:blank"));
    return ResponseEntity.badRequest().body(problem);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ProblemDetail> handleMethodArgumentNotValid(
      MethodArgumentNotValidException ex) {
    String detail =
        ex.getBindingResult().getFieldErrors().stream()
            .map(error -> error.getField() + ": " + error.getDefaultMessage())
            .collect(Collectors.joining("; "));
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
    problem.setTitle("Validation Failed");
    problem.setType(URI.create("about:blank"));
    return ResponseEntity.badRequest().body(problem);
  }

  @ExceptionHandler(CommitmentValidationException.class)
  public ResponseEntity<ProblemDetail> handleCommitmentValidation(
      CommitmentValidationException ex) {
    ProblemDetail problem =
        ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    problem.setTitle("Validation Failed");
    problem.setType(URI.create("about:blank"));
    return ResponseEntity.badRequest().body(problem);
  }

  @ExceptionHandler(InvalidPenaltyConfigException.class)
  public ResponseEntity<ProblemDetail> handleInvalidPenaltyConfig(
      InvalidPenaltyConfigException ex) {
    ProblemDetail problem =
        ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    problem.setTitle("Validation Failed");
    problem.setType(URI.create("about:blank"));
    return ResponseEntity.badRequest().body(problem);
  }

  @ExceptionHandler(ConcurrentCommitmentCapExceededException.class)
  public ResponseEntity<ProblemDetail> handleConcurrentCommitmentCap(
      ConcurrentCommitmentCapExceededException ex) {
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    problem.setTitle("Conflict");
    problem.setType(URI.create("about:blank"));
    return ResponseEntity.status(HttpStatus.CONFLICT).body(problem);
  }

  @ExceptionHandler(FulfillmentRejectedException.class)
  public ResponseEntity<ProblemDetail> handleFulfillmentRejected(FulfillmentRejectedException ex) {
    ProblemDetail problem =
        ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    problem.setTitle("Validation Failed");
    problem.setType(URI.create("about:blank"));
    return ResponseEntity.badRequest().body(problem);
  }

  @ExceptionHandler(CommitmentNotFoundException.class)
  public ResponseEntity<ProblemDetail> handleCommitmentNotFound(CommitmentNotFoundException ex) {
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    problem.setTitle("Not Found");
    problem.setType(URI.create("about:blank"));
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem);
  }

  @ExceptionHandler(EditWindowClosedException.class)
  public ResponseEntity<ProblemDetail> handleEditWindowClosed(EditWindowClosedException ex) {
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    problem.setTitle("Conflict");
    problem.setType(URI.create("about:blank"));
    return ResponseEntity.status(HttpStatus.CONFLICT).body(problem);
  }

  @ExceptionHandler(InvalidCommitmentStateException.class)
  public ResponseEntity<ProblemDetail> handleInvalidCommitmentState(
      InvalidCommitmentStateException ex) {
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    problem.setTitle("Conflict");
    problem.setType(URI.create("about:blank"));
    return ResponseEntity.status(HttpStatus.CONFLICT).body(problem);
  }

  @ExceptionHandler(UserNotFoundException.class)
  public ResponseEntity<ProblemDetail> handleUserNotFound(UserNotFoundException ex) {
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    problem.setTitle("Not Found");
    problem.setType(URI.create("about:blank"));
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem);
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ProblemDetail> handleConstraintViolation(ConstraintViolationException ex) {
    String detail =
        ex.getConstraintViolations().stream()
            .map(v -> v.getPropertyPath() + ": " + v.getMessage())
            .collect(Collectors.joining("; "));
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
    problem.setTitle("Validation Failed");
    problem.setType(URI.create("about:blank"));
    return ResponseEntity.badRequest().body(problem);
  }

  @ExceptionHandler(EmailAlreadyRegisteredException.class)
  public ResponseEntity<ProblemDetail> handleEmailAlreadyRegistered(
      EmailAlreadyRegisteredException ex) {
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    problem.setTitle("Conflict");
    problem.setType(URI.create("about:blank"));
    return ResponseEntity.status(HttpStatus.CONFLICT).body(problem);
  }

  @ExceptionHandler(InvalidCredentialsException.class)
  public ResponseEntity<ProblemDetail> handleInvalidCredentials(InvalidCredentialsException ex) {
    ProblemDetail problem =
        ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, ex.getMessage());
    problem.setTitle("Unauthorized");
    problem.setType(URI.create("about:blank"));
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(problem);
  }

  @ExceptionHandler(RateLimitExceededException.class)
  public ResponseEntity<ProblemDetail> handleRateLimitExceeded(RateLimitExceededException ex) {
    ProblemDetail problem =
        ProblemDetail.forStatusAndDetail(HttpStatus.TOO_MANY_REQUESTS, ex.getMessage());
    problem.setTitle("Too Many Requests");
    problem.setType(URI.create("about:blank"));
    ResponseEntity.BodyBuilder builder = ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS);
    if (ex.getRetryAfterSeconds() != null) {
      builder.header("Retry-After", String.valueOf(ex.getRetryAfterSeconds()));
    }
    return builder.body(problem);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ProblemDetail> handleGeneric(Exception ex) {
    ProblemDetail problem =
        ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error");
    problem.setTitle("Internal Server Error");
    problem.setType(URI.create("about:blank"));
    return ResponseEntity.internalServerError().body(problem);
  }
}
