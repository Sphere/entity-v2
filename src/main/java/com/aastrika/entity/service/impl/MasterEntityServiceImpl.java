package com.aastrika.entity.service.impl;

import com.aastrika.entity.common.EntitySheetHeaders;
import com.aastrika.entity.document.MasterEntityDocument;
import com.aastrika.entity.dto.EntitySheetRow;
import com.aastrika.entity.dto.response.ApiResponse;
import com.aastrika.entity.mapper.MasterEntityMapper;
import com.aastrika.entity.model.MasterEntity;
import com.aastrika.entity.reader.EntitySheetReader;
import com.aastrika.entity.reader.EntitySheetReaderFactory;
import com.aastrika.entity.repository.es.ElasticSearchEntityRepository;
import com.aastrika.entity.repository.jpa.MasterEntityRepository;
import com.aastrika.entity.service.MasterEntityService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
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

  @Override
  public List<MasterEntity> parseEntitySheet(MultipartFile entitySheet) {
    log.info("Parsing csv file..........");
    List<MasterEntity> entities = new ArrayList<>();

    try (BufferedReader reader =
            new BufferedReader(new InputStreamReader(entitySheet.getInputStream()));
        CSVParser csvParser =
            new CSVParser(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader().withTrim())) {

      for (CSVRecord record : csvParser) {
        String type = record.get(EntitySheetHeaders.TYPE);
        String name = record.get(EntitySheetHeaders.NAME);
        String description = record.get(EntitySheetHeaders.DESCRIPTION);
        String language = record.get(EntitySheetHeaders.LANGUAGE);

        String generatedCode =
            record.isMapped(EntitySheetHeaders.CODE)
                    && !record.get(EntitySheetHeaders.CODE).isBlank()
                ? record.get(EntitySheetHeaders.CODE)
                : type.substring(0, 1).toUpperCase() + System.currentTimeMillis(); // fallback

        MasterEntity entity = new MasterEntity();
        entity.setType(type);
        entity.setName(name);
        entity.setDescription(description);
        entity.setLanguageCode(language);
        entity.setCode(generatedCode);
        entity.setStatus("Active");
        entity.setLevel(generatedCode);
        entity.setLevelId(record.get(EntitySheetHeaders.LEVEL_ID));
        entity.setCreatedBy(record.get(EntitySheetHeaders.CREATED_BY));
        entity.setUpdatedBy(record.get(EntitySheetHeaders.UPDATED_BY));
        entity.setReviewedBy(record.get(EntitySheetHeaders.REVIEWED_BY));

        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        if (record.isMapped(EntitySheetHeaders.CREATED_DATE)
            && !record.get(EntitySheetHeaders.CREATED_DATE).isBlank()) {
          entity.setCreatedAt(df.parse(record.get(EntitySheetHeaders.CREATED_DATE)));
        }
        if (record.isMapped(EntitySheetHeaders.UPDATED_DATE)
            && !record.get(EntitySheetHeaders.UPDATED_DATE).isBlank()) {
          entity.setUpdatedAt(df.parse(record.get(EntitySheetHeaders.UPDATED_DATE)));
        }
        if (record.isMapped(EntitySheetHeaders.REVIEWED_DATE)
            && !record.get(EntitySheetHeaders.REVIEWED_DATE).isBlank()) {
          entity.setReviewedAt(df.parse(record.get(EntitySheetHeaders.REVIEWED_DATE)));
        }

        Map<String, Object> props = new HashMap<>();
        if (record.isMapped(EntitySheetHeaders.ADDITIONAL_PROPERTIES)
            && !record.get(EntitySheetHeaders.ADDITIONAL_PROPERTIES).isBlank()) {
          ObjectMapper mapper = new ObjectMapper();
          props.putAll(
              mapper.readValue(
                  record.get(EntitySheetHeaders.ADDITIONAL_PROPERTIES),
                  new TypeReference<Map<String, Object>>() {}));
        }
        entity.setAdditionalProperties(props);

        if ("competency".equalsIgnoreCase(type)) {
          // Save parent competency first
          MasterEntity savedEntity = masterEntityRepository.save(entity);
          MasterEntityDocument entityDocument = masterEntityMapper.toDocument(savedEntity);
          elasticSearchEntityRepository.save(entityDocument);

          List<Map<String, Object>> children = new ArrayList<>();
          List<String> levelIds = new ArrayList<>();

          // Competency level column mappings
          String[][] levelColumns = {
            {
              EntitySheetHeaders.COMPETENCY_LEVEL_1_NAME,
              EntitySheetHeaders.COMPETENCY_LEVEL_1_DESCRIPTION
            },
            {
              EntitySheetHeaders.COMPETENCY_LEVEL_2_NAME,
              EntitySheetHeaders.COMPETENCY_LEVEL_2_DESCRIPTION
            },
            {
              EntitySheetHeaders.COMPETENCY_LEVEL_3_NAME,
              EntitySheetHeaders.COMPETENCY_LEVEL_3_DESCRIPTION
            },
            {
              EntitySheetHeaders.COMPETENCY_LEVEL_4_NAME,
              EntitySheetHeaders.COMPETENCY_LEVEL_4_DESCRIPTION
            },
            {
              EntitySheetHeaders.COMPETENCY_LEVEL_5_NAME,
              EntitySheetHeaders.COMPETENCY_LEVEL_5_DESCRIPTION
            }
          };

          for (int i = 0; i < levelColumns.length; i++) {
            String levelNameCol = levelColumns[i][0];
            String levelDescCol = levelColumns[i][1];

            if (record.isMapped(levelNameCol) && !record.get(levelNameCol).isBlank()) {
              MasterEntity levelEntity = new MasterEntity();
              levelEntity.setType("level");
              levelEntity.setName(record.get(levelNameCol));
              levelEntity.setDescription(record.get(levelDescCol));
              levelEntity.setLanguageCode(language);
              levelEntity.setStatus("Active");
              levelEntity.setLevel("L" + (i + 1));
              levelEntity.setCode(savedEntity.getCode() + "_L" + (i + 1));

              Map<String, Object> levelProps = new HashMap<>();
              levelProps.put("parentCompetency", savedEntity.getName());
              levelEntity.setAdditionalProperties(levelProps);

              MasterEntity savedLevel = masterEntityRepository.save(levelEntity);
              MasterEntityDocument entityLevelDocument = masterEntityMapper.toDocument(savedLevel);
              elasticSearchEntityRepository.save(entityLevelDocument);

              levelIds.add(String.valueOf(savedLevel.getCode()));

              // Convert savedLevel to Map<String,Object> for children
              Map<String, Object> childMap = new HashMap<>();
              childMap.put("id", savedLevel.getId());
              childMap.put("type", savedLevel.getType());
              childMap.put("name", savedLevel.getName());
              childMap.put("description", savedLevel.getDescription());
              childMap.put("language", savedLevel.getLanguageCode());
              childMap.put("level", savedLevel.getLevel());
              childMap.put("levelId", savedLevel.getLevelId());
              childMap.put("code", savedLevel.getCode());
              childMap.put("status", savedLevel.getStatus());
              childMap.put("additionalProperties", savedLevel.getAdditionalProperties());

              children.add(childMap);
            }
          }

          // Save mapping between competency and levels
          String id = "COMPETENCY_LEVEL" + ":" + savedEntity.getCode();

          //                    Optional<EntityRelationship> existingOpt =
          // entityRelationshipRepository.findById(String.valueOf(savedEntity.getId()));
          //
          //                    EntityRelationship mapping =
          // existingOpt.orElseGet(EntityRelationship::new);
          //                    mapping.setId(id);
          //                    mapping.setType("COMPETENCY_LEVEL");
          //                    mapping.setParentId(String.valueOf(savedEntity.getCode()));
          //
          //                    // Merge or assign childIds
          //                    if (levelIds != null && !levelIds.isEmpty()) {
          //                        List<String> newChildren = new
          // ArrayList<>(existingOpt.map(EntityRelationship::getChildIds).orElse(List.of()));
          //                        newChildren.addAll(levelIds);
          //                        mapping.setChildIds(new ArrayList<>(newChildren.stream()
          //                                .distinct()
          //                                .collect(Collectors.toList())));
          //                    }
          //
          //                    entityRelationshipRepository.save(mapping);
          //
          //                    // Attach children for response
          //                    savedEntity.setChildren(children);

          entities.add(savedEntity); // add parent competency

        } else {

          if (entity.getCode() == null || entity.getCode().isBlank()) {
            entity.setCode(type.substring(0, 1).toUpperCase() + System.currentTimeMillis());
          }

          entity.setLevel(entity.getCode());
          entity.setLevelId("0");
          entity.setStatus("Active");

          MasterEntity savedEntity = masterEntityRepository.save(entity);
          MasterEntityDocument entityDocument = masterEntityMapper.toDocument(savedEntity);
          //                    EntityDocument entityDocument =
          // EntityMapper.INSTANCE.toDocument(entity);
          elasticSearchEntityRepository.save(entityDocument);

          entities.add(entity);
        }
      }
    } catch (Exception e) {
      throw new RuntimeException("Error parsing CSV", e);
    }

    return entities;
  }

  /** New method using EntitySheetReaderFactory - supports both CSV and XLSX */
  @Override
  public ApiResponse parseEntitySheetV2(MultipartFile file) {
    log.info("parseEntitySheetV2 - Using EntitySheetReaderFactory");

    // Factory finds the appropriate reader (CSV or XLSX) based on file type
    EntitySheetReader entitySheetReader = entitySheetReaderFactory.getSheetReader(file);
    log.info("Selected reader: {}", entitySheetReader.getClass().getSimpleName());

    // Read and return rows
    List<EntitySheetRow> rows = entitySheetReader.read(file);
    //        log.info("Parsed {} rows from file", rows.size());

    ApiResponse apiResponse = new ApiResponse();
    apiResponse.setResponseCode(HttpStatus.OK.name());
    apiResponse.setResult(rows);
    apiResponse.setTs(OffsetDateTime.now(ZoneId.of("Asia/Kolkata")));
    apiResponse.setVer("v1");
    apiResponse.setResponseCode(HttpStatus.OK.name());

    ApiResponse.Params params = new ApiResponse.Params();
    params.setStatus("success");

    apiResponse.setParams(params);

    return apiResponse;
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

  @Override
  @Transactional(readOnly = true)
  public Optional<MasterEntity> findById(Integer id) {
    return masterEntityRepository.findById(id);
  }

  @Override
  @Transactional(readOnly = true)
  public List<MasterEntity> findAll() {
    return masterEntityRepository.findAll();
  }

  @Override
  @Transactional(readOnly = true)
  public List<MasterEntity> findByType(String type) {
    return masterEntityRepository.findByType(type);
  }

  @Override
  @Transactional(readOnly = true)
  public List<MasterEntity> findByStatus(String status) {
    return masterEntityRepository.findByStatus(status);
  }

  @Override
  @Transactional(readOnly = true)
  public List<MasterEntity> findByTypeAndStatus(String type, String status) {
    return masterEntityRepository.findByTypeAndStatus(type, status);
  }

  @Override
  @Transactional(readOnly = true)
  public List<MasterEntity> search(String keyword) {
    List<MasterEntityDocument> documents =
        elasticSearchEntityRepository.findByNameContaining(keyword);
    List<Integer> ids = documents.stream().map(doc -> doc.getId()).toList();
    return masterEntityRepository.findAllById(ids);
  }

  @Override
  public void delete(Integer id) {
    masterEntityRepository.deleteById(id);
    elasticSearchEntityRepository.deleteById(String.valueOf(id));
  }

  private void indexToElasticsearch(MasterEntity entity) {
    MasterEntityDocument document =
        MasterEntityDocument.builder()
            .id(entity.getId())
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
