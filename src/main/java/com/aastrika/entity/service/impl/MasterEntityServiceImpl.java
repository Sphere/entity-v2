package com.aastrika.entity.service.impl;

import com.aastrika.entity.common.ApplicationConstants;
import com.aastrika.entity.common.EntitySheetHeadersConstant;
import com.aastrika.entity.document.MasterEntityDocument;
import com.aastrika.entity.dto.EntitySheetRow;
import com.aastrika.entity.dto.request.CompetencyLevelDTO;
import com.aastrika.entity.dto.request.EntityCreateRequestDTO;
import com.aastrika.entity.dto.request.EntityUpdateDTO;
import com.aastrika.entity.dto.response.AppResponse;
import com.aastrika.entity.dto.response.EntityResponseDTO;
import com.aastrika.entity.dto.response.EntityResult;
import com.aastrika.entity.exception.UpdateEntityException;
import com.aastrika.entity.exception.UploadEntityException;
import com.aastrika.entity.mapper.MasterEntityMapper;
import com.aastrika.entity.model.CompetencyLevel;
import com.aastrika.entity.model.MasterEntity;
import com.aastrika.entity.reader.EntitySheetReader;
import com.aastrika.entity.reader.EntitySheetReaderFactory;
import com.aastrika.entity.repository.es.ElasticSearchEntityRepository;
import com.aastrika.entity.repository.jpa.MasterEntityRepository;
import com.aastrika.entity.service.MasterEntityEsService;
import com.aastrika.entity.service.MasterEntityService;
import com.aastrika.entity.util.CodeLanguageProjection;
import com.aastrika.entity.util.EntityUtil;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.util.StringUtil;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class MasterEntityServiceImpl implements MasterEntityService {

  private final MasterEntityRepository masterEntityRepository;
  private final ElasticSearchEntityRepository elasticSearchEntityRepository;
  private final MasterEntityMapper masterEntityMapper;
  private final EntitySheetReaderFactory entitySheetReaderFactory;
  private final EntityUtil entityUtil;
  private final MasterEntityEsService masterEntityEsService;

  record EntityUploadTracker(String entityType, List<String> entityCode){}
  /**
   * @param sheetFile
   * @return
   */
  public AppResponse processAndUploadSheet(MultipartFile sheetFile, String userId) {
    EntitySheetReader entitySheetReader = entitySheetReaderFactory.getSheetReader(sheetFile);

    Map<String, List<EntitySheetRow>> entitySheetMap = entitySheetReader.getCompiledEntitySheet(sheetFile);

    if (StringUtil.isBlank(userId)) {
      throw new UploadEntityException(HttpStatus.BAD_REQUEST, "Missing user id in request");
    }

    EntityUploadTracker entityUploadTracker = saveSheetDataIntoDB(entitySheetMap,
      entitySheetReader.getGlobalEntityType(), userId);

    return AppResponse.success("api.entity.upload", EntityResult.of(entityUploadTracker), HttpStatus.OK);
  }

  /**
   * @param entitySheetMap
   * @param globalEntityType
   * @return
   */
  private EntityUploadTracker saveSheetDataIntoDB(Map<String, List<EntitySheetRow>> entitySheetMap,
                                                  String globalEntityType, String userId) {
    List<String> trackerEntityCodeList = new ArrayList<>();
    List<MasterEntity> masterEntities = new ArrayList<>();
    EntityUploadTracker entityUploadTracker = null;

    if (!StringUtil.isBlank(globalEntityType) && entitySheetMap.containsKey(globalEntityType.toUpperCase())) {
      List<EntitySheetRow> entitySheetRows = entitySheetMap.get(globalEntityType.toUpperCase());

      checkDuplicateSheetEntries(entitySheetRows);

      for (EntitySheetRow entitySheetRow : entitySheetRows) {
        MasterEntity masterEntity = masterEntityMapper.toEntity(entitySheetRow);
        masterEntity.setCreatedBy(userId);

        if (EntitySheetHeadersConstant.COMPETENCY_TYPE.equalsIgnoreCase(globalEntityType)) {
          List<CompetencyLevel> competencyLevels = entityUtil.getCompetencyListByEntity(entitySheetRow, masterEntity);
          masterEntity.setCompetencyLevels(competencyLevels);
        }

        masterEntities.add(masterEntity);
        trackerEntityCodeList.add(masterEntity.getCode());

        entityUploadTracker = new EntityUploadTracker(globalEntityType, trackerEntityCodeList);
      }

      masterEntityRepository.saveAll(masterEntities);
      masterEntityEsService.saveEntityDetailsInES(entitySheetRows, globalEntityType);
    }

    return entityUploadTracker;
  }

  /**
   * @param entitySheetRows
   */
  private void checkDuplicateSheetEntries(List<EntitySheetRow> entitySheetRows) {
    if (entitySheetRows != null && !entitySheetRows.isEmpty()) {
      List<String> codeLanguagePairs = entitySheetRows.stream()
        .map(row -> row.getCode() + ":" + row.getLanguage())
        .toList();

      List<CodeLanguageProjection> CodeLanguageProjectionList =
        masterEntityRepository.findByCodeLanguagePairs(codeLanguagePairs);

      if (CodeLanguageProjectionList != null && !CodeLanguageProjectionList.isEmpty()) {
        throw new UploadEntityException(HttpStatus.BAD_REQUEST, "Duplicate entry found", CodeLanguageProjectionList);
      }
    }
  }


  @Override
  public AppResponse create(EntityCreateRequestDTO entityCreateRequestDTO, String userId) {
    if (StringUtil.isBlank(userId)) {
      throw new UpdateEntityException(HttpStatus.BAD_REQUEST, "Unable to find user id details");
    }

    Optional<MasterEntity> masterEntityOptional =
      masterEntityRepository.findByCodeAndLanguageCode(entityCreateRequestDTO.getCode(),
        entityCreateRequestDTO.getLanguageCode());

    masterEntityOptional.ifPresent(entity -> {
      throw new UpdateEntityException(HttpStatus.CONFLICT,
        "Entity already exists with code: " + entityCreateRequestDTO.getCode()
          + " and language: " + entityCreateRequestDTO.getLanguageCode());
    });

    MasterEntity masterEntity = masterEntityMapper.toEntity(entityCreateRequestDTO);
    masterEntity.setCreatedAt(new Date());
    masterEntity.setCreatedBy(userId);

    if (ApplicationConstants.COMPETENCY_TYPE.equalsIgnoreCase(entityCreateRequestDTO.getEntityType())) {
      populateCompetencyInMasterEntity(masterEntity, entityCreateRequestDTO);
    }

    masterEntityRepository.save(masterEntity);
    upsertToElasticsearch(masterEntity);

    return AppResponse.success("api.entity.create", EntityResult.empty(), HttpStatus.OK);
  }

  private void populateCompetencyInMasterEntity(@NonNull MasterEntity masterEntity,
                                                @NonNull EntityCreateRequestDTO entityCreateRequestDTO) {
    if (entityCreateRequestDTO.getCompetencyLevels() != null && !entityCreateRequestDTO.getCompetencyLevels().isEmpty()) {
      List<CompetencyLevel> competencyLevelList = new ArrayList<>();

      for (CompetencyLevelDTO competencyLevelDTO : entityCreateRequestDTO.getCompetencyLevels()) {
        CompetencyLevel competencyLevel = masterEntityMapper.toCompetencyLevel(competencyLevelDTO);
        competencyLevel.setMasterEntity(masterEntity);
        competencyLevelList.add(competencyLevel);
      }
      masterEntity.setCompetencyLevels(competencyLevelList);
    }
  }

  /**
   * @param updateDTO
   * @param userId
   * @return
   */
  @Override
  public AppResponse update(EntityUpdateDTO updateDTO, String userId) {
    Optional<MasterEntity> existingMasterEntityOptional = masterEntityRepository
        .findByCodeAndLanguageCode(updateDTO.getCode(), updateDTO.getLanguageCode());

    MasterEntity existingMasterEntity = existingMasterEntityOptional.orElseThrow(() ->
      new UpdateEntityException(HttpStatus.NOT_FOUND, "Entity code " + updateDTO.getCode() +
        " with language " + updateDTO.getLanguageCode() + " not found for update"));

    if (StringUtil.isBlank(userId)) {
      throw new UpdateEntityException(HttpStatus.BAD_REQUEST, "Unable to find user id details");
    }

    // Update basic fields (only if provided in DTO)
    if (updateDTO.getEntityId() != null) {
      existingMasterEntity.setEntityId(updateDTO.getEntityId());
    }
    if (updateDTO.getEntityType() != null) {
      existingMasterEntity.setEntityType(updateDTO.getEntityType());
    }
    if (updateDTO.getType() != null) {
      existingMasterEntity.setType(updateDTO.getType());
    }
    if (updateDTO.getArea() != null) {
      existingMasterEntity.setArea(updateDTO.getArea());
    }
    if (updateDTO.getName() != null) {
      existingMasterEntity.setName(updateDTO.getName());
    }
    if (updateDTO.getDescription() != null) {
      existingMasterEntity.setDescription(updateDTO.getDescription());
    }
    if (updateDTO.getStatus() != null) {
      existingMasterEntity.setStatus(updateDTO.getStatus());
    }
    if (updateDTO.getLevel() != null) {
      existingMasterEntity.setLevel(updateDTO.getLevel());
    }
    if (updateDTO.getLevelId() != null) {
      existingMasterEntity.setLevelId(updateDTO.getLevelId());
    }
    if (updateDTO.getSource() != null) {
      existingMasterEntity.setSource(updateDTO.getSource());
    }
    if (updateDTO.getAdditionalProperties() != null) {
      existingMasterEntity.setAdditionalProperties(updateDTO.getAdditionalProperties());
    }

    // Update competency levels if provided
    if (updateDTO.getCompetencyLevels() != null && ApplicationConstants.COMPETENCY_TYPE.equalsIgnoreCase(updateDTO.getEntityType())) {
      updateCompetencyLevel(existingMasterEntity, updateDTO);
    }

    existingMasterEntity.setUpdatedAt(new Date());
    existingMasterEntity.setUpdatedBy(userId);

    MasterEntity updatedMasterEntity = masterEntityRepository.save(existingMasterEntity);
    upsertToElasticsearch(updatedMasterEntity);
    return getUpdatedWrappedResponse(updatedMasterEntity);
  }

  private AppResponse getUpdatedWrappedResponse(MasterEntity updatedMasterEntity) {
    EntityResponseDTO entityResponseDTO = masterEntityMapper.toResponseDTO(updatedMasterEntity);
    return AppResponse.success("api.entity.update", EntityResult.of(entityResponseDTO), HttpStatus.OK);
  }

  /**
   * @param existingMasterEntity
   * @param entityUpdateDTO
   */
  private void updateCompetencyLevel(MasterEntity existingMasterEntity, EntityUpdateDTO entityUpdateDTO) {
    for (CompetencyLevelDTO competencyLevelDTO : entityUpdateDTO.getCompetencyLevels()) {
      Optional<CompetencyLevel> existingCompetencyLevelOptional = existingMasterEntity.getCompetencyLevels().stream()
        .filter(competencyLevel -> competencyLevel.getLevelNumber().equals(competencyLevelDTO.getLevelNumber()))
        .findFirst();

      if (existingCompetencyLevelOptional.isPresent()) {
        CompetencyLevel competencyLevel = existingCompetencyLevelOptional.get();
        competencyLevel.setLevelName(competencyLevelDTO.getLevelName());
        competencyLevel.setLevelDescription(competencyLevelDTO.getLevelDescription());
//        existingMasterEntity.getCompetencyLevels().add(existingCompetencyLevelOptional.get());
      } else {
        CompetencyLevel level = CompetencyLevel.builder()
          .masterEntity(existingMasterEntity)
          .levelNumber(competencyLevelDTO.getLevelNumber())
          .levelName(competencyLevelDTO.getLevelName())
          .levelDescription(competencyLevelDTO.getLevelDescription())
          .build();
        existingMasterEntity.getCompetencyLevels().add(level);
      }
    }
  }

  @Transactional(readOnly = true)
  @Override
  public List<MasterEntity> findByCode(String code) {
    return masterEntityRepository.findByCode(code);
  }

  @Override
  @Transactional(readOnly = true)
  public List<MasterEntity> findAll() {
    return masterEntityRepository.findAll();
  }

  @Override
  @Transactional(readOnly = true)
  public List<MasterEntity> findByEntityType(String type) {
    return masterEntityRepository.findByEntityType(type);
  }

  @Override
  public void delete(Integer id) {
    masterEntityRepository.deleteById(id);
    elasticSearchEntityRepository.deleteById(String.valueOf(id));
  }

  private void upsertToElasticsearch(MasterEntity entity) {
    // Use code + languageCode as unique ES document ID
    String esDocId = entity.getCode() + "_" + entity.getLanguageCode();

    MasterEntityDocument document = MasterEntityDocument.builder()
        .id(esDocId)
        .entityId(entity.getEntityId())
        .entityType(entity.getEntityType())
        .type(entity.getType())
        .code(entity.getCode())
        .name(entity.getName())
        .description(entity.getDescription())
        .status(entity.getStatus())
        .languageCode(entity.getLanguageCode())
        .level(entity.getLevel())
        .levelId(entity.getLevelId())
        .additionalProperties(entity.getAdditionalProperties())
        .createdAt(entity.getCreatedAt())
        .updatedAt(entity.getUpdatedAt())
        .createdBy(entity.getCreatedBy())
        .updatedBy(entity.getUpdatedBy())
        .build();

    // Map competency levels if present
    if (entity.getCompetencyLevels() != null && !entity.getCompetencyLevels().isEmpty()) {
      for (CompetencyLevel level : entity.getCompetencyLevels()) {
        switch (level.getLevelNumber()) {
          case 1 -> {
            document.setCompetencyLevel1Name(level.getLevelName());
            document.setCompetencyLevel1Description(level.getLevelDescription());
          }
          case 2 -> {
            document.setCompetencyLevel2Name(level.getLevelName());
            document.setCompetencyLevel2Description(level.getLevelDescription());
          }
          case 3 -> {
            document.setCompetencyLevel3Name(level.getLevelName());
            document.setCompetencyLevel3Description(level.getLevelDescription());
          }
          case 4 -> {
            document.setCompetencyLevel4Name(level.getLevelName());
            document.setCompetencyLevel4Description(level.getLevelDescription());
          }
          case 5 -> {
            document.setCompetencyLevel5Name(level.getLevelName());
            document.setCompetencyLevel5Description(level.getLevelDescription());
          }
        }
      }
    }

    elasticSearchEntityRepository.save(document);
  }
}
