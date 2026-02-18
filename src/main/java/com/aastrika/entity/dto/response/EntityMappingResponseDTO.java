package com.aastrika.entity.dto.response;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EntityMappingResponseDTO {
  private String parentEntityType;

  private String parentEntityCode;

  private String childEntityType;

  private String childEntityCode;

  private List<Integer> competencies;
}
