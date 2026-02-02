package com.aastrika.entity.mapper;

import com.aastrika.entity.document.MasterEntityDocument;
import com.aastrika.entity.dto.EntitySheetRow;
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

  MasterEntityDocument toDocument(MasterEntity entity);

  MasterEntity toEntity(EntitySheetRow entitySheetRow);
}
