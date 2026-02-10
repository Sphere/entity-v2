package com.aastrika.entity.mapper;

import com.aastrika.entity.document.MasterEntityDocument;
import com.aastrika.entity.dto.EntitySheetRow;
import com.aastrika.entity.dto.response.CompetencyLevelResponseDTO;
import com.aastrika.entity.dto.response.EntityResponseDTO;
import com.aastrika.entity.model.CompetencyLevel;
import com.aastrika.entity.model.MasterEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

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
}
