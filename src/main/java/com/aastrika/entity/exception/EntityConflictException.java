package com.aastrika.entity.exception;

import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

@NoArgsConstructor
public class EntityConflictException extends ApiRuntimeException {

  public EntityConflictException(HttpStatus httpStatus, String errorMessage) {
    super(httpStatus, errorMessage);
  }

  public EntityConflictException(HttpStatus httpStatus, String errorMessage, Object result) {
    super(httpStatus, errorMessage, result);
  }

  public EntityConflictException(HttpStatus httpStatus, String errorMessage, String error) {
    super(httpStatus, errorMessage);
  }
}
