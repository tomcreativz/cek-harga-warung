package com.alpha.products.AlphaProducts.exception;

import com.alpha.products.AlphaProducts.model.response.ErrorResponse;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(ProductNotFoundException.class)
  public ResponseEntity<ErrorResponse> notFound(ProductNotFoundException e) {
    return build(HttpStatus.NOT_FOUND, e.getMessage());
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ErrorResponse> constraint(ConstraintViolationException e) {
    String msg = e.getConstraintViolations().stream()
        .map(v -> v.getMessage()).sorted().reduce((a, b) -> a + "; " + b).orElse("Input tidak valid");
    return build(HttpStatus.BAD_REQUEST, msg);
  }

  @ExceptionHandler(HandlerMethodValidationException.class)
  public ResponseEntity<ErrorResponse> methodValidation(HandlerMethodValidationException e) {
    String msg = e.getAllErrors().stream()
        .map(err -> err.getDefaultMessage()).sorted().reduce((a, b) -> a + "; " + b)
        .orElse("Input tidak valid");
    return build(HttpStatus.BAD_REQUEST, msg);
  }

  @ExceptionHandler(MissingServletRequestParameterException.class)
  public ResponseEntity<ErrorResponse> missingParam(MissingServletRequestParameterException e) {
    return build(HttpStatus.BAD_REQUEST, "Parameter '" + e.getParameterName() + "' wajib diisi");
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<ErrorResponse> typeMismatch(MethodArgumentTypeMismatchException e) {
    return build(HttpStatus.BAD_REQUEST, "Parameter '" + e.getName() + "' tidak valid");
  }

  private ResponseEntity<ErrorResponse> build(HttpStatus status, String message) {
    return ResponseEntity.status(status)
        .body(ErrorResponse.of(status.value(), status.getReasonPhrase(), message));
  }
}
