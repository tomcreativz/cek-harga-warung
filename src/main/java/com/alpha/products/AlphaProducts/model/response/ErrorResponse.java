package com.alpha.products.AlphaProducts.model.response;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ErrorResponse {
  private final int status;
  private final String error;
  private final String message;
  private final Instant timestamp;

  public static ErrorResponse of(int status, String error, String message) {
    return new ErrorResponse(status, error, message, Instant.now());
  }
}
