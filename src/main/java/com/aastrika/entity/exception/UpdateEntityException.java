package com.aastrika.entity.exception;

import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

@NoArgsConstructor
public class UpdateEntityException extends ApiRuntimeException {

  public UpdateEntityException(HttpStatus httpStatus, String errorMessage) {
    super(httpStatus, errorMessage);
  }

  public UpdateEntityException(HttpStatus httpStatus, String errorMessage, Object result) {
    super(httpStatus, errorMessage, result);
  }

  public UpdateEntityException(HttpStatus httpStatus, String errorMessage, String error) {
    super(httpStatus, errorMessage);
  }
}
