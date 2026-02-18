package com.aastrika.entity.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChildMappingDTO {
  @NotBlank(message = "childId must not be blank")
  private String childCode;
  private List<CompetencyLevelDTO> competencies;
}
