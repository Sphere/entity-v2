package com.aastrika.entity.dto.response;

import com.aastrika.entity.dto.request.CompetencyLevelDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FullHierarchyNodeDTO {

  private String entityType;

  private String entityCode;

  private String entityName;

  private String entityDescription;

  private String language;

  private List<CompetencyLevelDTO> competencies;

  private List<FullHierarchyNodeDTO> children;
}
