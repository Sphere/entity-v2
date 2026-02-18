package com.aastrika.entity.service.impl;

import com.aastrika.entity.common.ApplicationConstants;
import com.aastrika.entity.dto.request.CompetencyLevelDTO;
import com.aastrika.entity.dto.request.EntityMappingRequestDTO;
import com.aastrika.entity.dto.request.EntitySearchRequestDTO;
import com.aastrika.entity.dto.response.EntityChildHierarchyDTO;
import com.aastrika.entity.dto.response.EntityMappingResponseDTO;
import com.aastrika.entity.dto.response.HierarchyResponseDTO;
import com.aastrika.entity.exception.MissingMappingDataException;
import com.aastrika.entity.exception.UpdateEntityException;
import com.aastrika.entity.mapper.EntityMapMapper;
import com.aastrika.entity.model.EntityMap;
import com.aastrika.entity.model.MasterEntity;
import com.aastrika.entity.repository.jpa.EntityMapRepository;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.aastrika.entity.repository.jpa.MasterEntityRepository;
import com.aastrika.entity.service.EntityMappingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EntityMappingServiceImpl implements EntityMappingService {

  private final EntityMapRepository entityMapRepository;
  private final EntityMapMapper entityMapMapper;
  private final MasterEntityRepository masterEntityRepository;

  /**
   * @param entityMappingRequestDTOList
   * @return
   */
  @Override
  @Transactional
  public List<EntityMappingResponseDTO> saveEntityMapping(List<EntityMappingRequestDTO> entityMappingRequestDTOList) {
    List<EntityMap> entityMaps = new ArrayList<>();

    if (entityMappingRequestDTOList != null && !entityMappingRequestDTOList.isEmpty()) {
      for (EntityMappingRequestDTO entityMappingRequestDTO : entityMappingRequestDTOList) {

        Optional<EntityMap> entityMapOptional =entityMapRepository.findByParentEntityCodeAndChildEntityCode(
          entityMappingRequestDTO.getParentEntityCode(),
          entityMappingRequestDTO.getChildEntityCode()
        );

        EntityMap entityMap;
        if (entityMapOptional.isPresent()) {
          entityMap = entityMapOptional.get();
          entityMap.setParentEntityCode(entityMappingRequestDTO.getParentEntityCode());
          entityMap.setParentEntityType(entityMappingRequestDTO.getParentEntityType());
          entityMap.setChildEntityCode(entityMappingRequestDTO.getChildEntityCode());
          entityMap.setChildEntityType(entityMap.getChildEntityType());

        } else {
          entityMap = entityMapMapper.toEntity(entityMappingRequestDTO);
        }
        entityMap.setCompetencyLevelList(getCompetencyLevelSeries(entityMappingRequestDTO.getCompetencies()));
        entityMaps.add(entityMap);
      }

      List<EntityMap> savedEntityMapList = entityMapRepository.saveAll(entityMaps);
      return entityMapMapper.toResponseDTOList(savedEntityMapList);
    }
    return List.of();
  }

  private String getCompetencyLevelSeries(List<Integer> competencyLevels) {
    String levelSeries = "";
    if (competencyLevels != null || !competencyLevels.isEmpty()) {
      levelSeries = competencyLevels.stream()
        .map(String::valueOf)
        .collect(Collectors.joining(","));
    }
    return levelSeries;
  }

  /**
   * @param entitySearchRequestDTO
   * @return
   */
  @Override
  public List<HierarchyResponseDTO> getEntityMappingHierarchy(EntitySearchRequestDTO entitySearchRequestDTO) {
    List<HierarchyResponseDTO> hierarchyResponseDTOList = new ArrayList<>();

    List<EntityMap> entityMapList =
      entityMapRepository.findByParentEntityCodeAndParentEntityType(entitySearchRequestDTO.getEntityCode(),
        entitySearchRequestDTO.getEntityType());

    if (entityMapList != null && !entityMapList.isEmpty()) {
      Optional<MasterEntity> masterEntityOptional =
        masterEntityRepository.findByCodeAndLanguageCode(entitySearchRequestDTO.getEntityCode(),
          entitySearchRequestDTO.getEntityLanguage());

      MasterEntity masterEntity = masterEntityOptional.orElseThrow(() -> new MissingMappingDataException(HttpStatus.BAD_REQUEST,
        "Entity mapping details is missing"));

      HierarchyResponseDTO hierarchyResponseDTO = HierarchyResponseDTO.builder()
        .entityType(masterEntity.getEntityType())
        .entityCode(masterEntity.getCode())
        .entityName(masterEntity.getName())
        .language(masterEntity.getLanguageCode())
        .build();

     List<String> childEntityCodeList = entityMapList.stream()
       .map(EntityMap::getChildEntityCode)
       .map(String::toUpperCase)
       .toList();

     hierarchyResponseDTO.setChildHierarchy(collectChildHierarchyDetails(childEntityCodeList, masterEntity.getLanguageCode()));
      hierarchyResponseDTOList.add(hierarchyResponseDTO);
    }

    return hierarchyResponseDTOList;
  }


  private List<EntityChildHierarchyDTO> collectChildHierarchyDetails(List<String> childEntityCodeList, String language) {
    List<MasterEntity> masterEntityList = masterEntityRepository.findByCodeInAndLanguageCode(childEntityCodeList,
      language);

    if (masterEntityList != null && !masterEntityList.isEmpty()) {
      List<EntityChildHierarchyDTO> entityChildHierarchyDTOList = masterEntityList.stream()
        .map(masterEntity -> {
          EntityChildHierarchyDTO childHierarchyDto = new EntityChildHierarchyDTO();
          childHierarchyDto.setEntityType(masterEntity.getEntityType());
          childHierarchyDto.setEntityCode(masterEntity.getCode());
          childHierarchyDto.setEntityName(masterEntity.getName());

          if (ApplicationConstants.COMPETENCY_TYPE.equalsIgnoreCase(masterEntity.getEntityType())) {
            childHierarchyDto.setCompetencies(masterEntity.getCompetencyLevels().stream()
              .map(competencyLevel -> CompetencyLevelDTO.builder()
                .levelNumber(competencyLevel.getLevelNumber())
                .levelName(competencyLevel.getLevelName())
                .build())
              .toList());
          }
          return childHierarchyDto;
        }).toList();

      return entityChildHierarchyDTOList;
    }

    return Collections.emptyList();
  }
}
