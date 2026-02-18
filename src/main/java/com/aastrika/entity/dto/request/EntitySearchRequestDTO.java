package com.aastrika.entity.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EntitySearchRequestDTO {
  @NotBlank(message = "Entity type must not be blank")
  private String entityType;

  @NotBlank(message = "Entity code must not be blank")
  private String entityCode;

  @NotBlank(message = "Entity language must not be blank")
  private String entityLanguage;
}
