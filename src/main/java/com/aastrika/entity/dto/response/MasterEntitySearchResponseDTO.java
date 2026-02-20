package com.aastrika.entity.dto.response;

import com.aastrika.entity.dto.request.CompetencyLevelDTO;
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
public class MasterEntitySearchResponseDTO {

  private String id;
  private String entityId;
  private String entityType;
  private String type;
  private String code;
  private String name;
  private String description;
  private String status;
  private String languageCode;
  private String level;
  private String levelId;
  private Map<String, Object> additionalProperties;
  private Date createdAt;
  private Date updatedAt;
  private String createdBy;
  private String updatedBy;
  private List<CompetencyLevelDTO> levels;
}
