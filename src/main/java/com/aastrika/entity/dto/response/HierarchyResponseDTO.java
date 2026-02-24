package com.aastrika.entity.dto.response;

import com.aastrika.entity.dto.request.CompetencyLevelDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HierarchyResponseDTO {
  private String entityType;

  private String entityCode;

  private String entityName;

  private String language;

  private String entityDescription;

  private List<EntityChildHierarchyDTO> childHierarchy;
}
