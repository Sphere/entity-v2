package com.aastrika.entity.exception;

import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

@NoArgsConstructor
public class SheetDataMissingException extends ApiRuntimeException {

    public SheetDataMissingException(HttpStatus httpStatus, String errorMessage, Object result) {
        super(httpStatus, errorMessage, result);
    }

    public SheetDataMissingException(HttpStatus httpStatus, String errorMessage) {
        super(httpStatus, errorMessage);
    }
}
