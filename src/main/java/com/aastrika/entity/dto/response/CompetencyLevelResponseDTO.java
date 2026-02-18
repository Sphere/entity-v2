package com.aastrika.entity.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompetencyLevelResponseDTO {

//  private Integer id;
  private Integer levelNumber;
  private String levelName;
  private String levelDescription;
}
