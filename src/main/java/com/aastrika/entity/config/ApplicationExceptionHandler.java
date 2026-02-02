package com.aastrika.entity.config;

import com.aastrika.entity.dto.response.ApiResponse;
import com.aastrika.entity.exception.ApiRuntimeException;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.coyote.BadRequestException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.OffsetDateTime;
import java.time.ZoneId;

@RestControllerAdvice
public class ApplicationExceptionHandler {

    @ExceptionHandler(ApiRuntimeException.class)
    public ResponseEntity<ApiResponse> handleBadRequest(ApiRuntimeException exception,
                                                           HttpServletRequest httpServletRequest) {

        ApiResponse response = new ApiResponse<>();
        response.setId(exception.getApiId());
        response.setVer("v1");
        response.setTs(OffsetDateTime.now(ZoneId.of("Asia/Kolkata")));
        response.setResponseCode(exception.getStatus().toString());
        if (exception.getResult() != null) {
            response.setResult(exception.getResult());
        }
        ApiResponse.Params params = new ApiResponse.Params();
        params.setErrmsg(exception.getMessage());
        params.setStatus(exception.getStatus().toString());
        response.setParams(params);

        return ResponseEntity
                .status(exception.getStatus())
                .body(response);
    }
}
