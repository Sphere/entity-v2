package com.aastrika.entity.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EntityCreateRequestDTO {

  // Identifier (required to find the entity)
  @NotBlank(message = "Entity code must not be blank")
  private String code;

  @NotBlank(message = "Entity language must not be blank")
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
