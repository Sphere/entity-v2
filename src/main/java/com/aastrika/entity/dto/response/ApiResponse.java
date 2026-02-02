package com.aastrika.entity.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ApiResponse<T> {
    private String id;
    private String ver;
    private OffsetDateTime ts;
    private Params params;
    private String responseCode;
    private T result;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Params {
        private String resmsgid;
        private String msgid;
        private String err;
        private String status;
        private String errmsg;
    }
}
