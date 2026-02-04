package com.aastrika.entity.service.impl;

import com.aastrika.entity.common.EntitySheetHeadersConstant;
import com.aastrika.entity.document.MasterEntityDocument;
import com.aastrika.entity.dto.EntitySheetRow;
import com.aastrika.entity.dto.response.ApiResponse;
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
  public ApiResponse processAndUploadSheet(MultipartFile sheetFile) {
    EntitySheetReader entitySheetReader = entitySheetReaderFactory.getSheetReader(sheetFile);

    Map<String, List<EntitySheetRow>> entitySheetMap = entitySheetReader.getCompiledEntitySheet(sheetFile);

    EntityUploadTracker entityUploadTracker = saveSheetDataIntoDB(entitySheetMap, entitySheetReader.getGlobalEntityType());

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
                                                  String globalEntityType) {
    List<String> trackerEntityCodeList = new ArrayList<>();
    List<MasterEntity> masterEntities = new ArrayList<>();
    EntityUploadTracker entityUploadTracker = null;

    if (!StringUtil.isBlank(globalEntityType) && entitySheetMap.containsKey(globalEntityType.toUpperCase())) {
      List<EntitySheetRow> entitySheetRows = entitySheetMap.get(globalEntityType.toUpperCase());

      checkDuplicateSheetEntries(entitySheetRows);

      for (EntitySheetRow entitySheetRow : entitySheetRows) {
        MasterEntity masterEntity = masterEntityMapper.toEntity(entitySheetRow);

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

//  @Transactional(readOnly = true)
//  public List<MasterEntity> search(String keyword) {
//    List<MasterEntityDocument> documents =
//        elasticSearchEntityRepository.findByNameContaining(keyword);
//    List<Integer> ids = documents.stream().map(doc -> doc.getId()).toList();
//    return masterEntityRepository.findAllById(ids);
//  }

  @Override
  public void delete(Integer id) {
    masterEntityRepository.deleteById(id);
    elasticSearchEntityRepository.deleteById(String.valueOf(id));
  }

  private void indexToElasticsearch(MasterEntity entity) {
    MasterEntityDocument document =
        MasterEntityDocument.builder()
//            .id(entity.getId())
            .type(entity.getType())
            .name(entity.getName())
            .description(entity.getDescription())
            .status(entity.getStatus())
            .createdAt(entity.getCreatedAt())
            .updatedAt(entity.getUpdatedAt())
            .createdBy(entity.getCreatedBy())
            .updatedBy(entity.getUpdatedBy())
            .build();
    elasticSearchEntityRepository.save(document);
  }
}
