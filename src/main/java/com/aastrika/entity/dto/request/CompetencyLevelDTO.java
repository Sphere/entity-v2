package com.aastrika.entity.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompetencyLevelDTO {

  private Integer levelNumber;
  private String levelName;
  private String levelDescription;
}
