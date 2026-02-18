package com.aastrika.entity.exception;

import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

@NoArgsConstructor
public class MissingMappingDataException extends ApiRuntimeException {

  public MissingMappingDataException(HttpStatus httpStatus, String errorMessage, Object result) {
    super(httpStatus, errorMessage, result);
  }

  public MissingMappingDataException(HttpStatus httpStatus, String errorMessage) {
    super(httpStatus, errorMessage);
  }
}
