package com.aastrika.entity.dto;

import java.util.Date;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EntitySheetRow {
  private String rowNumber;
  private String entityId;
  private String entityType;
  private String type;
  private String name;
  private String description;
  private String language;
  private String code;
  private String area;
  private String levelId;
  private String createdBy;
  private String updatedBy;
  private String reviewedBy;
  private Date createdDate;
  private String updatedDate;
  private String reviewedDate;
  private Map<String, Object> additionalProperties;

  // Competency levels
  private String competencyLevel1Name;
  private String competencyLevel1Description;
  private String competencyLevel2Name;
  private String competencyLevel2Description;
  private String competencyLevel3Name;
  private String competencyLevel3Description;
  private String competencyLevel4Name;
  private String competencyLevel4Description;
  private String competencyLevel5Name;
  private String competencyLevel5Description;
}
