package com.aastrika.entity.exception;

import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

@NoArgsConstructor
public class DataMissingException extends ApiRuntimeException {

  public DataMissingException(HttpStatus httpStatus, String errorMessage, Object result) {
    super(httpStatus, errorMessage, result);
  }

  public DataMissingException(HttpStatus httpStatus, String errorMessage) {
    super(httpStatus, errorMessage);
  }
}
