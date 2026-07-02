package com.aastrika.entity.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EntityDeleteRequestDTO {

  @NotBlank(message = "entityCode is required")
  @Setter(AccessLevel.NONE)
  private String entityCode;

  @NotBlank(message = "entityType is required")
  private String entityType;

  private String language;

  private Boolean purgeAllLanguage = false;

  public void setEntityCode(String entityCode) {
    this.entityCode = entityCode != null ? entityCode.toUpperCase() : null;
  }
}
