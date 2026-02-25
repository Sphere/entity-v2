package com.aastrika.entity.mapper;

import com.aastrika.entity.document.MasterEntityDocument;
import com.aastrika.entity.dto.EntitySheetRow;
import com.aastrika.entity.enums.EntityType;
import com.aastrika.entity.dto.request.CompetencyLevelDTO;
import com.aastrika.entity.dto.request.EntityCreateRequestDTO;
import com.aastrika.entity.dto.response.CompetencyLevelResponseDTO;
import com.aastrika.entity.dto.response.EntityResponseDTO;
import com.aastrika.entity.dto.response.MasterEntitySearchResponseDTO;
import com.aastrika.entity.model.CompetencyLevel;
import com.aastrika.entity.model.MasterEntity;
import java.util.ArrayList;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface MasterEntityMapper {

  @Mapping(target = "source", ignore = true)
  @Mapping(target = "level", ignore = true)
  @Mapping(target = "levelId", ignore = true)
  @Mapping(target = "reviewedAt", ignore = true)
  @Mapping(target = "reviewedBy", ignore = true)
  MasterEntity toEntity(MasterEntityDocument document);

  @Mapping(source = "language", target = "languageCode")
  @Mapping(source = "createdDate", target = "createdAt")
  @Mapping(source = "entityType", target = "entityType", qualifiedByName = "stringToEntityType")
  @Mapping(target = "updatedAt", ignore = true)
  @Mapping(target = "reviewedAt", ignore = true)
  @Mapping(target = "source", ignore = true)
  @Mapping(target = "level", ignore = true)
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "competencyLevels", ignore = true)
  MasterEntity toEntity(EntitySheetRow entitySheetRow);

  @Mapping(source = "language", target = "languageCode")
  @Mapping(source = "createdDate", target = "createdAt")
  @Mapping(target = "updatedAt", ignore = true)
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "level", ignore = true)
  @Mapping(target = "status", constant = "Active")
  MasterEntityDocument toDocument(EntitySheetRow entitySheetRow);

  EntityResponseDTO toResponseDTO(MasterEntity entity);

  CompetencyLevelResponseDTO toResponseDTO(CompetencyLevel level);

  CompetencyLevel toCompetencyLevel(CompetencyLevelDTO competencyLevelDTO);

  MasterEntity toEntity(EntityCreateRequestDTO entityCreateRequestDTO);

  @Mapping(target = "levels", ignore = true)
  MasterEntitySearchResponseDTO toSearchResponseDTO(MasterEntityDocument document);

  default MasterEntitySearchResponseDTO toSearchResponse(MasterEntityDocument document) {
    MasterEntitySearchResponseDTO dto = toSearchResponseDTO(document);
    dto.setLevels(mapCompetencyLevels(document));
    return dto;
  }

  @Named("stringToEntityType")
  default EntityType stringToEntityType(String value) {
    return EntityType.fromValue(value);
  }

  default List<CompetencyLevelDTO> mapCompetencyLevels(MasterEntityDocument document) {
    List<CompetencyLevelDTO> levels = new ArrayList<>();

    if (document.getCompetencyLevel1Name() != null) {
      levels.add(CompetencyLevelDTO.builder()
          .levelNumber(1).levelName(document.getCompetencyLevel1Name())
          .levelDescription(document.getCompetencyLevel1Description()).build());
    }
    if (document.getCompetencyLevel2Name() != null) {
      levels.add(CompetencyLevelDTO.builder()
          .levelNumber(2).levelName(document.getCompetencyLevel2Name())
          .levelDescription(document.getCompetencyLevel2Description()).build());
    }
    if (document.getCompetencyLevel3Name() != null) {
      levels.add(CompetencyLevelDTO.builder()
          .levelNumber(3).levelName(document.getCompetencyLevel3Name())
          .levelDescription(document.getCompetencyLevel3Description()).build());
    }
    if (document.getCompetencyLevel4Name() != null) {
      levels.add(CompetencyLevelDTO.builder()
          .levelNumber(4).levelName(document.getCompetencyLevel4Name())
          .levelDescription(document.getCompetencyLevel4Description()).build());
    }
    if (document.getCompetencyLevel5Name() != null) {
      levels.add(CompetencyLevelDTO.builder()
          .levelNumber(5).levelName(document.getCompetencyLevel5Name())
          .levelDescription(document.getCompetencyLevel5Description()).build());
    }

    return levels;
  }
}
