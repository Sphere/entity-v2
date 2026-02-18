package com.aastrika.entity.dto.response;

import com.aastrika.entity.dto.request.CompetencyLevelDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EntityChildHierarchyDTO {
  private String entityType;

  private String entityCode;

  private String entityName;

  private List<CompetencyLevelDTO> competencies;
}
