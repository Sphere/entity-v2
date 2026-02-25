package com.aastrika.entity.dto.request;

import com.aastrika.entity.enums.EntityType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EntityMappingRequestDTO {
  @NotNull(message = "parent entity type must not be blank")
  private EntityType parentEntityType;

  @NotBlank(message = "parent entity code must not be blank")
  @Setter(AccessLevel.NONE)
  private String parentEntityCode;

  @NotNull(message = "child entity type must not be blank")
  private EntityType childEntityType;

  @NotBlank(message = "child entity code must not be blank")
  @Setter(AccessLevel.NONE)
  private String childEntityCode;

  private List<Integer> competencies;

  public void setParentEntityCode(String parentEntityCode) {
    this.parentEntityCode = parentEntityCode != null ? parentEntityCode.toUpperCase() : null;
  }

  public void setChildEntityCode(String childEntityCode) {
    this.childEntityCode = childEntityCode != null ? childEntityCode.toUpperCase() : null;
  }
}
