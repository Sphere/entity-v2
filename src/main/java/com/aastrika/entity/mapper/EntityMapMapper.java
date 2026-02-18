package com.aastrika.entity.mapper;

import com.aastrika.entity.dto.request.EntityMappingRequestDTO;
import com.aastrika.entity.dto.response.EntityMappingResponseDTO;
import com.aastrika.entity.model.EntityMap;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface EntityMapMapper {

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "competencyLevelList", ignore = true)
  EntityMap toEntity(EntityMappingRequestDTO dto);

  @Mapping(source = "competencyLevelList", target = "competencies", qualifiedByName = "toCompetencyList")
  EntityMappingResponseDTO toResponseDTO(EntityMap entityMap);

  List<EntityMappingResponseDTO> toResponseDTOList(List<EntityMap> entityMaps);

  @Named("toCompetencyList")
  default List<Integer> toCompetencyList(String competencyLevelList) {
    if (competencyLevelList == null || competencyLevelList.isBlank()) {
      return Collections.emptyList();
    }
    return Arrays.stream(competencyLevelList.split(","))
        .map(String::trim)
        .map(Integer::valueOf)
        .toList();
  }
}