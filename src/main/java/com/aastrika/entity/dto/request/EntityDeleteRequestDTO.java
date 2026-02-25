package com.aastrika.entity.dto.request;

import com.aastrika.entity.enums.EntityType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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

  @NotNull(message = "entityType is required")
  private EntityType entityType;

  private String language;

  private Boolean purgeAllLanguage = false;

  public void setEntityCode(String entityCode) {
    this.entityCode = entityCode != null ? entityCode.toUpperCase() : null;
  }
}
