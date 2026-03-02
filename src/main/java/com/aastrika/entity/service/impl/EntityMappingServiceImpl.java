package com.aastrika.entity.service.impl;

import com.aastrika.entity.enums.EntityType;
import com.aastrika.entity.dto.request.CompetencyLevelDTO;
import com.aastrika.entity.dto.request.EntityMappingRequestDTO;
import com.aastrika.entity.dto.request.EntitySearchRequestDTO;
import com.aastrika.entity.dto.response.EntityChildHierarchyDTO;
import com.aastrika.entity.dto.response.EntityMappingResponseDTO;
import com.aastrika.entity.dto.response.FullHierarchyNodeDTO;
import com.aastrika.entity.dto.response.HierarchyResponseDTO;
import com.aastrika.entity.exception.MissingMappingDataException;
import com.aastrika.entity.exception.UpdateEntityException;
import com.aastrika.entity.mapper.CompetencyLevelMapper;
import com.aastrika.entity.mapper.EntityMapMapper;
import com.aastrika.entity.model.EntityMap;
import com.aastrika.entity.model.MasterEntity;
import com.aastrika.entity.repository.jpa.EntityMapRepository;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.aastrika.entity.repository.jpa.MasterEntityRepository;
import com.aastrika.entity.service.EntityMappingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.util.StringUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class EntityMappingServiceImpl implements EntityMappingService {

  private final EntityMapRepository entityMapRepository;
  private final EntityMapMapper entityMapMapper;
  private final MasterEntityRepository masterEntityRepository;
  private final CompetencyLevelMapper competencyLevelMapper;

//  @Value("${entity-map.allowed-type-combinations}")
//  private Set<String> allowedTypeCombinations;

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

//        validateMappingStructure(entityMappingRequestDTO.getParentEntityType(),
//            entityMappingRequestDTO.getChildEntityType());
        validateEntityExists(entityMappingRequestDTO.getParentEntityCode(),
            entityMappingRequestDTO.getParentEntityType(), "Parent");
        validateEntityExists(entityMappingRequestDTO.getChildEntityCode(),
            entityMappingRequestDTO.getChildEntityType(), "Child");

        List<EntityMap> entityMapList =entityMapRepository.findByParentEntityCodeAndParentEntityType(
          entityMappingRequestDTO.getParentEntityCode(),
          entityMappingRequestDTO.getParentEntityType()
        );

        if (entityMapList != null && !entityMapList.isEmpty()) {
          List<Integer> entityMapIds = entityMapList.stream()
              .map(EntityMap::getId)
                .toList();
          entityMapRepository.deleteAllById(entityMapIds);
          entityMapRepository.flush();
        }

        EntityMap entityMap = entityMapMapper.toEntity(entityMappingRequestDTO);
        if (EntityType.COMPETENCY == entityMappingRequestDTO.getChildEntityType()) {
          entityMap.setCompetencyLevelList(getCompetencyLevelSeries(entityMappingRequestDTO.getCompetencies()));
        }
        entityMaps.add(entityMap);
      }

      List<EntityMap> savedEntityMapList = entityMapRepository.saveAll(entityMaps);
      return entityMapMapper.toResponseDTOList(savedEntityMapList);
    }
    return List.of();
  }

//  private void validateMappingStructure(EntityType parentType, EntityType childType) {
//    String combination = parentType.name() + "_" + childType.name();
//    if (!allowedTypeCombinations.contains(combination)) {
//      throw new UpdateEntityException(HttpStatus.BAD_REQUEST,
//          "Invalid mapping structure: " + combination + ". Allowed combinations: " + allowedTypeCombinations);
//    }
//  }

  private void validateEntityExists(String code, EntityType entityType, String role) {
    List<MasterEntity> entities = masterEntityRepository.findByCodeAndEntityType(code, entityType);
    if (entities.isEmpty()) {
      throw new UpdateEntityException(HttpStatus.NOT_FOUND,
          role + " entity not found with code: " + code + " and type: " + entityType.name());
    }
  }

  private String getCompetencyLevelSeries(List<Integer> competencyLevels) {
    String levelSeries = "";
    if (competencyLevels != null && !competencyLevels.isEmpty()) {
      levelSeries = competencyLevels.stream()
        .map(String::valueOf)
        .collect(Collectors.joining(","));
    }
    return levelSeries;
  }

  @Override
  public FullHierarchyNodeDTO getFullHierarchy(EntitySearchRequestDTO entitySearchRequestDTO) {
    // Build adjacency map level by level — only loads the relevant subtree
    Map<String, List<EntityMap>> adjacencyMap = new HashMap<>();
    Set<String> allCodes = new HashSet<>();
    allCodes.add(entitySearchRequestDTO.getEntityCode());

    Set<String> currentLevel = new HashSet<>();
    currentLevel.add(entitySearchRequestDTO.getEntityCode());

    while (!currentLevel.isEmpty()) {
      List<EntityMap> mappings = entityMapRepository.findByParentEntityCodeIn(new ArrayList<>(currentLevel));
      Set<String> nextLevel = new HashSet<>();
      for (EntityMap mapping : mappings) {
        adjacencyMap.computeIfAbsent(mapping.getParentEntityCode(), k -> new ArrayList<>()).add(mapping);
        if (allCodes.add(mapping.getChildEntityCode())) {
          nextLevel.add(mapping.getChildEntityCode());
        }
      }
      currentLevel = nextLevel;
    }

    log.debug("BFS allCodes: {}", allCodes);
    log.debug("adjacencyMap keys: {}", adjacencyMap.keySet());

    // Batch fetch all entity details for the collected codes
    Map<String, MasterEntity> entityLookup = masterEntityRepository
        .findByCodeInAndLanguageCode(new ArrayList<>(allCodes), entitySearchRequestDTO.getEntityLanguage())
        .stream()
        .collect(Collectors.toMap(MasterEntity::getCode, e -> e));

    log.debug("entityLookup keys: {}", entityLookup.keySet());

    return buildNodeFromMemory(
        entitySearchRequestDTO.getEntityCode(),
        entitySearchRequestDTO.getEntityType(),
        null,
        adjacencyMap,
        entityLookup
    );
  }

  private FullHierarchyNodeDTO buildNodeFromMemory(String code, EntityType entityType,
                                                   String competencyLevelList,
                                                   Map<String, List<EntityMap>> adjacencyMap,
                                                   Map<String, MasterEntity> entityLookup) {
    MasterEntity entity = entityLookup.get(code);
    if (entity == null) return null;

    FullHierarchyNodeDTO node = FullHierarchyNodeDTO.builder()
        .entityType(entityType != null ? entityType.name() : null)
        .entityCode(entity.getCode())
        .entityName(entity.getName())
        .entityDescription(entity.getDescription())
        .language(entity.getLanguageCode())
        .build();

    if (EntityType.COMPETENCY == entityType && entity.getCompetencyLevels() != null) {
      List<Integer> applicableLevels = convertStringifyCompetencyToInt(competencyLevelList);
      if (!applicableLevels.isEmpty()) {
        node.setCompetencies(competencyLevelMapper.toCompetencyLevelDTOList(
            entity.getCompetencyLevels().stream()
                .filter(cl -> applicableLevels.contains(cl.getLevelNumber()))
                .toList()));
      }
    }

    List<EntityMap> childMappings = adjacencyMap.getOrDefault(code, List.of());
    if (!childMappings.isEmpty()) {
      List<FullHierarchyNodeDTO> children = childMappings.stream()
          .map(em -> buildNodeFromMemory(em.getChildEntityCode(), em.getChildEntityType(),
              em.getCompetencyLevelList(), adjacencyMap, entityLookup))
          .toList();
      node.setChildren(children);
    }

    return node;
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

    hierarchyResponseDTOList.add(buildParentChildHierarchyDetails(entitySearchRequestDTO.getEntityCode(),
      entitySearchRequestDTO.getEntityLanguage(), entityMapList));

    return hierarchyResponseDTOList;
  }

  /**
   * Fetches full hierarchy details — parent entity, all child entities,
   * and competency levels for each child if the child is of competency type.
   *
   * @param parentEntityCode    parent entity code
   * @param language      language code for fetching entities
   * @param entityMapList list of entity mappings for the parent
   * @return HierarchyResponseDTO with parent info and populated child hierarchy
   */
  private HierarchyResponseDTO buildParentChildHierarchyDetails(String parentEntityCode, String language,
                                                                List<EntityMap> entityMapList) {
    // 1. Fetch parent entity
    MasterEntity parentEntity = masterEntityRepository.findByCodeAndLanguageCode(parentEntityCode, language)
      .orElseThrow(() -> new MissingMappingDataException(HttpStatus.BAD_REQUEST, "Parent entity not found"));

    // 2. Collect child entity codes from mappings
    List<String> childEntityCodes = entityMapList.stream()
      .map(EntityMap::getChildEntityCode)
      .map(String::toUpperCase)
      .toList();

    // 3. Build a lookup map: childEntityCode → applicable competency level numbers from EntityMap
    Map<String, List<Integer>> childCompetencyMap = entityMapList.stream()
      .collect(Collectors.toMap(
        entityMap -> entityMap.getChildEntityCode().toUpperCase(),
        entityMap -> convertStringifyCompetencyToInt(entityMap.getCompetencyLevelList())
      ));

    // 4. Fetch all child entities in one query
    List<MasterEntity> childEntities = masterEntityRepository.findByCodeInAndLanguageCode(childEntityCodes, language);

    // 5. Build child hierarchy DTOs — filter competency levels by those defined in EntityMap
    List<EntityChildHierarchyDTO> childHierarchyList = childEntities.stream()
      .map(childEntity -> {
        EntityChildHierarchyDTO childDto = new EntityChildHierarchyDTO();
        childDto.setEntityType(childEntity.getEntityType() != null ? childEntity.getEntityType().name() : null);
        childDto.setEntityCode(childEntity.getCode());
        childDto.setEntityName(childEntity.getName());
        childDto.setEntityDescription(childEntity.getDescription());

        if (EntityType.COMPETENCY == childEntity.getEntityType()
          && childEntity.getCompetencyLevels() != null) {
          List<Integer> applicableLevels = childCompetencyMap.getOrDefault(
            childEntity.getCode().toUpperCase(), List.of());

          childDto.setCompetencies(
            competencyLevelMapper.toCompetencyLevelDTOList(
              childEntity.getCompetencyLevels().stream()
                .filter(cl -> applicableLevels.contains(cl.getLevelNumber()))
                .toList()));
        }

        return childDto;
      })
      .toList();

    // 5. Build and return the full hierarchy response
    return HierarchyResponseDTO.builder()
      .entityType(parentEntity.getEntityType() != null ? parentEntity.getEntityType().name() : null)
      .entityCode(parentEntity.getCode())
      .entityName(parentEntity.getName())
      .language(parentEntity.getLanguageCode())
      .entityDescription(parentEntity.getDescription())
      .childHierarchy(childHierarchyList)
      .build();
  }


  private List<Integer> convertStringifyCompetencyToInt(String competency) {
    if (!StringUtil.isBlank(competency)) {
      return Arrays.stream(competency.split(","))
        .filter(s -> !s.isBlank())
        .map(String::trim)
        .filter(s -> s.matches("-?\\d+"))
        .map(Integer::valueOf)
        .toList();
    }
    return Collections.emptyList();
  }

}
