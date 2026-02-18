package com.aastrika.entity.config;

import com.aastrika.entity.dto.response.AppResponse;
import com.aastrika.entity.exception.ApiRuntimeException;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApplicationExceptionHandler {

  @ExceptionHandler(ApiRuntimeException.class)
  public ResponseEntity<AppResponse> handleBadRequest(ApiRuntimeException exception) {
    AppResponse response = AppResponse.error(exception.getApiId(), exception.getMessage(), exception.getStatus());
    if (exception.getResult() != null) {
      response.setResult(exception.getResult());
    }
    return ResponseEntity.status(exception.getStatus()).body(response);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<AppResponse> handleValidationException(MethodArgumentNotValidException exception) {
    String errorMessages = exception.getBindingResult().getFieldErrors().stream()
        .map(error -> error.getField() + ": " + error.getDefaultMessage())
        .collect(Collectors.joining(", "));

    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(AppResponse.error(null, errorMessages, HttpStatus.BAD_REQUEST));
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<AppResponse> handleDataIntegrityViolation(DataIntegrityViolationException exception) {
    String message = "Data integrity violation";
    Throwable rootCause = exception.getRootCause();
    if (rootCause != null) {
      message = rootCause.getMessage();
    }

    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(AppResponse.error(null, message, HttpStatus.CONFLICT));
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<AppResponse> handleIllegalArgument(IllegalArgumentException exception) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(AppResponse.error(null, exception.getMessage(), HttpStatus.BAD_REQUEST));
  }
}
