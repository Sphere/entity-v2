package com.aastrika.entity.mapper;

import com.aastrika.entity.dto.request.CompetencyLevelDTO;
import com.aastrika.entity.model.CompetencyLevel;
import java.util.List;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CompetencyLevelMapper {

  CompetencyLevelDTO toCompetencyLevelDTO(CompetencyLevel level);

  List<CompetencyLevelDTO> toCompetencyLevelDTOList(List<CompetencyLevel> levels);
}
