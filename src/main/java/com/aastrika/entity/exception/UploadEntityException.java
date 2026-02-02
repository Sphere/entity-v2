package com.aastrika.entity.exception;

import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

@NoArgsConstructor
public class UploadEntityException extends ApiRuntimeException {

  public UploadEntityException(HttpStatus httpStatus, String errorMessage) {
    super(httpStatus, errorMessage);
  }

  public UploadEntityException(HttpStatus httpStatus, String errorMessage, Object result) {
    super(httpStatus, errorMessage, result);
  }

  public UploadEntityException(HttpStatus httpStatus, String errorMessage, String error) {
    super(httpStatus, errorMessage);
  }
}
