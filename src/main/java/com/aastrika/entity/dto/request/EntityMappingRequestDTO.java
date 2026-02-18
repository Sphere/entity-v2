package com.aastrika.entity.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EntityMappingRequestDTO {
  @NotBlank(message = "parent entity type must not be blank")
  private String parentEntityType;

  @NotBlank(message = "parent entity code must not be blank")
  private String parentEntityCode;

  @NotBlank(message = "child entity type must not be blank")
  private String childEntityType;

  @NotBlank(message = "child entity code must not be blank")
  private String childEntityCode;

  private List<Integer> competencies;
}
