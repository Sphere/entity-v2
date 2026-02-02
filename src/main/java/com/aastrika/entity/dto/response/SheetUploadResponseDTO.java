package com.aastrika.entity.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SheetUploadResponseDTO {
    private String status;

    private Map<String, String> rowColumnStatus;

    private Map<String, Integer> headerStatus;
}
