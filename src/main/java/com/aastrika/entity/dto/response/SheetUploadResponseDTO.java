package com.aastrika.entity.dto.response;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SheetUploadResponseDTO {
  private String status;

  private Map<String, String> rowColumnStatus;

  private Map<String, Integer> headerStatus;
}
