package com.aastrika.entity.dto.response;

import java.util.Date;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EntityResponseDTO {

  private Integer id;
  private String entityId;
  private String entityType;
  private String type;
  private String area;
  private String code;
  private String name;
  private String description;
  private String status;
  private String languageCode;
  private String level;
  private String levelId;
  private String source;
  private Map<String, Object> additionalProperties;

  private Date createdAt;
  private Date updatedAt;
  private String createdBy;
  private String updatedBy;

  private List<CompetencyLevelResponseDTO> competencyLevels;
}
