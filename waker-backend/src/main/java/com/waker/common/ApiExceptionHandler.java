package com.waker.common;

import jakarta.validation.ConstraintViolationException;
import java.net.URI;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
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

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ProblemDetail> handleGeneric(Exception ex) {
    ProblemDetail problem =
        ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error");
    problem.setTitle("Internal Server Error");
    problem.setType(URI.create("about:blank"));
    return ResponseEntity.internalServerError().body(problem);
  }
}
