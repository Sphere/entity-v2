package com.aastrika.entity.exception;

import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

@NoArgsConstructor
public class HeaderMissingException extends ApiRuntimeException {

    public HeaderMissingException(HttpStatus httpStatus, String errorMessage, Object result) {
        super(httpStatus, errorMessage, result);
    }

    public HeaderMissingException(HttpStatus httpStatus, String errorMessage) {
        super(httpStatus, errorMessage);
    }
}
