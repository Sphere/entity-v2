package com.aastrika.entity.dto.request;

import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EntityUpdateDTO {

  // Identifier (required to find the entity)
  private String code;
  private String languageCode;

  // Updatable fields
  private String entityId;
  private String entityType;
  private String type;
  private String area;
  private String name;
  private String description;
  private String status;
  private String level;
  private String levelId;
  private String source;
  private Map<String, Object> additionalProperties;

  // Competency levels
  private List<CompetencyLevelDTO> competencyLevels;
}
