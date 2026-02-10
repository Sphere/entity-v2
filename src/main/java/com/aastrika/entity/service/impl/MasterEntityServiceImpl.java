package com.aastrika.entity.service.impl;

import com.aastrika.entity.common.ApplicationConstants;
import com.aastrika.entity.common.EntitySheetHeadersConstant;
import com.aastrika.entity.document.MasterEntityDocument;
import com.aastrika.entity.dto.EntitySheetRow;
import com.aastrika.entity.dto.request.CompetencyLevelDTO;
import com.aastrika.entity.dto.request.EntityUpdateDTO;
import com.aastrika.entity.dto.response.ApiResponse;
import com.aastrika.entity.dto.response.EntityResponseDTO;
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
  public ApiResponse processAndUploadSheet(MultipartFile sheetFile, String userId) {
    EntitySheetReader entitySheetReader = entitySheetReaderFactory.getSheetReader(sheetFile);

    Map<String, List<EntitySheetRow>> entitySheetMap = entitySheetReader.getCompiledEntitySheet(sheetFile);

    if (StringUtil.isBlank(userId)) {
      throw new UploadEntityException(HttpStatus.BAD_REQUEST, "Missing user id in request");
    }

    EntityUploadTracker entityUploadTracker = saveSheetDataIntoDB(entitySheetMap,
      entitySheetReader.getGlobalEntityType(), userId);

    ApiResponse apiResponse = new ApiResponse();
    apiResponse.setResponseCode(HttpStatus.OK.name());
    apiResponse.setResult(entityUploadTracker);
    apiResponse.setTs(OffsetDateTime.now(ZoneId.of("Asia/Kolkata")));
    apiResponse.setVer("v1");
    apiResponse.setResponseCode(HttpStatus.OK.name());

    ApiResponse.Params params = new ApiResponse.Params();
    params.setStatus("success");

    apiResponse.setParams(params);

    return apiResponse;

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
  public MasterEntity create(MasterEntity entity) {
    entity.setCreatedAt(new Date());
    MasterEntity saved = masterEntityRepository.save(entity);
    indexToElasticsearch(saved);
    return saved;
  }

  @Override
  public MasterEntity update(Integer id, MasterEntity entity) {
    MasterEntity existing =
        masterEntityRepository
            .findById(id)
            .orElseThrow(() -> new RuntimeException("Entity not found with id: " + id));

    existing.setType(entity.getType());
    existing.setName(entity.getName());
    existing.setDescription(entity.getDescription());
    existing.setAdditionalProperties(entity.getAdditionalProperties());
    existing.setStatus(entity.getStatus());
    existing.setSource(entity.getSource());
    existing.setLevel(entity.getLevel());
    existing.setLevelId(entity.getLevelId());
    existing.setLanguageCode(entity.getLanguageCode());
    existing.setUpdatedAt(new Date());
    existing.setUpdatedBy(entity.getUpdatedBy());
    existing.setReviewedAt(entity.getReviewedAt());
    existing.setReviewedBy(entity.getReviewedBy());

    MasterEntity updated = masterEntityRepository.save(existing);
    indexToElasticsearch(updated);
    return updated;
  }


  /**
   * @param updateDTO
   * @param userId
   * @return
   */
  @Override
  public ApiResponse update(EntityUpdateDTO updateDTO, String userId) {
    MasterEntity existingMasterEntity = masterEntityRepository
        .findByCodeAndLanguageCode(updateDTO.getCode(), updateDTO.getLanguageCode());

    if (StringUtil.isBlank(updateDTO.getCode()) || StringUtil.isBlank(updateDTO.getLanguageCode())) {
      throw new UpdateEntityException(HttpStatus.BAD_REQUEST, "Entity code or Language code is missing");
    }

    if (StringUtil.isBlank(userId)) {
      throw new UpdateEntityException(HttpStatus.BAD_REQUEST, "Unable to find user id details");
    }

    if (existingMasterEntity == null) {
      throw new UpdateEntityException(HttpStatus.BAD_REQUEST,
          "Entity not found with code: " + updateDTO.getCode() + " and languageCode: " + updateDTO.getLanguageCode());
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
    indexToElasticsearch(updatedMasterEntity);
    return getUpdatedWrappedResponse(updatedMasterEntity);
  }

  private ApiResponse getUpdatedWrappedResponse(MasterEntity updatedMasterEntity) {
    EntityResponseDTO entityResponseDTO = masterEntityMapper.toResponseDTO(updatedMasterEntity);

    ApiResponse apiResponse = new ApiResponse();
    apiResponse.setResponseCode(HttpStatus.OK.name());
    apiResponse.setResult(entityResponseDTO);
    apiResponse.setTs(OffsetDateTime.now(ZoneId.of("Asia/Kolkata")));
    apiResponse.setVer("v1");
    apiResponse.setResponseCode(HttpStatus.OK.name());

    ApiResponse.Params params = new ApiResponse.Params();
    params.setStatus("success");

    apiResponse.setParams(params);

    return apiResponse;
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

  private void indexToElasticsearch(MasterEntity entity) {
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
