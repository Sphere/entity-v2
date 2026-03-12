package com.aastrika.entity.service.impl;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aastrika.entity.document.MasterEntityDocument;
import com.aastrika.entity.dto.EntitySheetRow;
import com.aastrika.entity.dto.request.CompetencyLevelDTO;
import com.aastrika.entity.dto.request.EntityCreateRequestDTO;
import com.aastrika.entity.dto.request.EntityDeleteRequestDTO;
import com.aastrika.entity.dto.request.EntityUpdateDTO;
import com.aastrika.entity.dto.response.AppResponse;
import com.aastrika.entity.dto.response.EntityResponseDTO;
import com.aastrika.entity.enums.EntityType;
import com.aastrika.entity.exception.UpdateEntityException;
import com.aastrika.entity.exception.UploadEntityException;
import com.aastrika.entity.mapper.MasterEntityMapper;
import com.aastrika.entity.model.CompetencyLevel;
import com.aastrika.entity.model.MasterEntity;
import com.aastrika.entity.reader.EntitySheetReader;
import com.aastrika.entity.reader.EntitySheetReaderFactory;
import com.aastrika.entity.repository.es.ElasticSearchEntityRepository;
import com.aastrika.entity.repository.jpa.EntityMapRepository;
import com.aastrika.entity.repository.jpa.MasterEntityRepository;
import com.aastrika.entity.service.MasterEntityEsService;
import com.aastrika.entity.util.EntityUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(MockitoExtension.class)
class MasterEntityServiceImplTest {

  @Mock private MasterEntityRepository masterEntityRepository;
  @Mock private ElasticSearchEntityRepository elasticSearchEntityRepository;
  @Mock private EntityMapRepository entityMapRepository;
  @Mock private MasterEntityMapper masterEntityMapper;
  @Mock private EntitySheetReaderFactory entitySheetReaderFactory;
  @Mock private EntityUtil entityUtil;
  @Mock private MasterEntityEsService masterEntityEsService;
  @Mock private EntitySheetReader entitySheetReader;

  @InjectMocks private MasterEntityServiceImpl masterEntityService;

  // ─── processAndUploadSheet ───────────────────────────────────────────────────

  @Test
  @DisplayName("processAndUploadSheet - should throw BAD_REQUEST when userId is blank")
  void shouldThrowWhenUserIdIsBlankOnUpload() {
    MultipartFile mockFile = mock(MultipartFile.class);
    when(entitySheetReaderFactory.getSheetReader(mockFile)).thenReturn(entitySheetReader);
    when(entitySheetReader.getCompiledEntitySheet(mockFile)).thenReturn(Map.of());

    UploadEntityException ex = assertThrows(UploadEntityException.class,
        () -> masterEntityService.processAndUploadSheet(mockFile, ""));

    assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    verify(masterEntityRepository, never()).saveAll(anyList());
  }

  @Test
  @DisplayName("processAndUploadSheet - should save non-competency entities and call ES service")
  void shouldUploadNonCompetencySheetSuccessfully() {
    MultipartFile mockFile = mock(MultipartFile.class);
    EntitySheetRow row = EntitySheetRow.builder()
        .code("R001").language("en").entityType("ROLE").name("Developer").build();
    MasterEntity mappedEntity = MasterEntity.builder()
        .code("R001").languageCode("en").entityType(EntityType.ROLE).name("Developer").build();
    List<EntitySheetRow> rows = List.of(row);
    Map<String, List<EntitySheetRow>> sheetMap = Map.of("ROLE", rows);

    when(entitySheetReaderFactory.getSheetReader(mockFile)).thenReturn(entitySheetReader);
    when(entitySheetReader.getCompiledEntitySheet(mockFile)).thenReturn(sheetMap);
    when(entitySheetReader.getGlobalEntityType()).thenReturn("ROLE");
    when(masterEntityRepository.findByCodeLanguagePairs(anyList())).thenReturn(List.of());
    when(masterEntityMapper.toEntity(any(EntitySheetRow.class))).thenReturn(mappedEntity);
    when(masterEntityRepository.saveAll(anyList())).thenReturn(List.of(mappedEntity));

    AppResponse result = masterEntityService.processAndUploadSheet(mockFile, "admin");

    assertNotNull(result);
    assertEquals(HttpStatus.OK.getReasonPhrase(), result.getResponseCode());
    verify(masterEntityRepository, times(1)).saveAll(anyList());
    verify(masterEntityEsService, times(1)).saveEntityDetailsInES(rows, "ROLE");
    verify(entityUtil, never()).getCompetencyListByEntity(any(), any());
  }

  @Test
  @DisplayName("processAndUploadSheet - should populate competency levels when type is COMPETENCY")
  void shouldUploadCompetencySheetSuccessfully() {
    MultipartFile mockFile = mock(MultipartFile.class);
    EntitySheetRow row = EntitySheetRow.builder()
        .code("C001").language("en").entityType("COMPETENCY").name("Communication").build();
    MasterEntity mappedEntity = MasterEntity.builder()
        .code("C001").languageCode("en").entityType(EntityType.COMPETENCY).name("Communication").build();
    List<CompetencyLevel> levels = List.of(
        CompetencyLevel.builder().levelNumber(1).levelName("Basic").levelDescription("Beginner level").build());
    List<EntitySheetRow> rows = List.of(row);
    Map<String, List<EntitySheetRow>> sheetMap = Map.of("COMPETENCY", rows);

    when(entitySheetReaderFactory.getSheetReader(mockFile)).thenReturn(entitySheetReader);
    when(entitySheetReader.getCompiledEntitySheet(mockFile)).thenReturn(sheetMap);
    when(entitySheetReader.getGlobalEntityType()).thenReturn("COMPETENCY");
    when(masterEntityRepository.findByCodeLanguagePairs(anyList())).thenReturn(List.of());
    when(masterEntityMapper.toEntity(any(EntitySheetRow.class))).thenReturn(mappedEntity);
    when(entityUtil.getCompetencyListByEntity(eq(row), eq(mappedEntity))).thenReturn(levels);
    when(masterEntityRepository.saveAll(anyList())).thenReturn(List.of(mappedEntity));

    AppResponse result = masterEntityService.processAndUploadSheet(mockFile, "admin");

    assertNotNull(result);
    assertEquals(HttpStatus.OK.getReasonPhrase(), result.getResponseCode());
    verify(entityUtil, times(1)).getCompetencyListByEntity(row, mappedEntity);
    assertEquals(levels, mappedEntity.getCompetencyLevels());
  }

  // ─── create ──────────────────────────────────────────────────────────────────

  @Test
  @DisplayName("create - should save entity and index to ES successfully")
  void shouldCreateEntitySuccessfully() {
    EntityCreateRequestDTO requestDTO = new EntityCreateRequestDTO();
    requestDTO.setCode("PS001");
    requestDTO.setLanguageCode("en");
    requestDTO.setEntityType(EntityType.ROLE);
    requestDTO.setType("Role");
    requestDTO.setName("Problem Solving");
    requestDTO.setDescription("Ability to solve problems");
    requestDTO.setStatus("Active");

    MasterEntity mappedEntity = MasterEntity.builder()
        .name("Problem Solving").type("Role").entityType(EntityType.ROLE)
        .description("Ability to solve problems").code("PS001")
        .languageCode("en").status("Active").build();

    when(masterEntityRepository.findByCodeAndLanguageCode("PS001", "en")).thenReturn(Optional.empty());
    when(masterEntityMapper.toEntity(any(EntityCreateRequestDTO.class))).thenReturn(mappedEntity);
    when(masterEntityRepository.save(any(MasterEntity.class))).thenReturn(mappedEntity);

    AppResponse result = masterEntityService.create(requestDTO, "admin");

    assertAll(
        () -> assertNotNull(result),
        () -> assertEquals(HttpStatus.OK.getReasonPhrase(), result.getResponseCode())
    );
    assertNotNull(mappedEntity.getCreatedAt(), "createdAt should be set before save");
    assertEquals("admin", mappedEntity.getCreatedBy(), "createdBy should match userId");
    verify(masterEntityRepository, times(1)).save(mappedEntity);

    ArgumentCaptor<MasterEntityDocument> esCaptor = ArgumentCaptor.forClass(MasterEntityDocument.class);
    verify(elasticSearchEntityRepository, times(1)).save(esCaptor.capture());
    MasterEntityDocument indexedDoc = esCaptor.getValue();
    assertAll(
        () -> assertEquals("PS001_en", indexedDoc.getId()),
        () -> assertEquals("Role", indexedDoc.getType()),
        () -> assertEquals("Problem Solving", indexedDoc.getName()),
        () -> assertEquals("Ability to solve problems", indexedDoc.getDescription())
    );
  }

  @Test
  @DisplayName("create - should throw CONFLICT when entity already exists")
  void shouldThrowExceptionWhenEntityAlreadyExists() {
    EntityCreateRequestDTO requestDTO = new EntityCreateRequestDTO();
    requestDTO.setCode("PS001");
    requestDTO.setLanguageCode("en");

    when(masterEntityRepository.findByCodeAndLanguageCode("PS001", "en"))
        .thenReturn(Optional.of(MasterEntity.builder().id(1).code("PS001").languageCode("en").build()));

    UpdateEntityException ex = assertThrows(UpdateEntityException.class,
        () -> masterEntityService.create(requestDTO, "admin"));

    assertEquals(HttpStatus.CONFLICT, ex.getStatus());
    verify(masterEntityRepository, never()).save(any(MasterEntity.class));
    verify(elasticSearchEntityRepository, never()).save(any(MasterEntityDocument.class));
  }

  @Test
  @DisplayName("create - should throw BAD_REQUEST when userId is blank")
  void shouldThrowExceptionWhenUserIdIsBlank() {
    EntityCreateRequestDTO requestDTO = new EntityCreateRequestDTO();
    requestDTO.setCode("PS001");
    requestDTO.setLanguageCode("en");

    UpdateEntityException ex = assertThrows(UpdateEntityException.class,
        () -> masterEntityService.create(requestDTO, ""));

    assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    verify(masterEntityRepository, never()).findByCodeAndLanguageCode(anyString(), anyString());
    verify(masterEntityRepository, never()).save(any(MasterEntity.class));
  }

  @Test
  @DisplayName("create - should populate competency levels when entity type is COMPETENCY")
  void shouldCreateCompetencyEntityWithLevels() {
    CompetencyLevelDTO levelDTO = CompetencyLevelDTO.builder()
        .levelNumber(1).levelName("Basic").levelDescription("Beginner level").build();
    EntityCreateRequestDTO requestDTO = new EntityCreateRequestDTO();
    requestDTO.setCode("C001");
    requestDTO.setLanguageCode("en");
    requestDTO.setEntityType(EntityType.COMPETENCY);
    requestDTO.setCompetencyLevels(List.of(levelDTO));

    MasterEntity mappedEntity = MasterEntity.builder()
        .code("C001").languageCode("en").entityType(EntityType.COMPETENCY)
        .competencyLevels(new ArrayList<>()).build();
    CompetencyLevel mappedLevel = CompetencyLevel.builder()
        .levelNumber(1).levelName("Basic").levelDescription("Beginner level").build();

    when(masterEntityRepository.findByCodeAndLanguageCode("C001", "en")).thenReturn(Optional.empty());
    when(masterEntityMapper.toEntity(any(EntityCreateRequestDTO.class))).thenReturn(mappedEntity);
    when(masterEntityMapper.toCompetencyLevel(any(CompetencyLevelDTO.class))).thenReturn(mappedLevel);
    when(masterEntityRepository.save(any(MasterEntity.class))).thenReturn(mappedEntity);

    AppResponse result = masterEntityService.create(requestDTO, "admin");

    assertNotNull(result);
    assertEquals(HttpStatus.OK.getReasonPhrase(), result.getResponseCode());
    assertEquals(1, mappedEntity.getCompetencyLevels().size());
    assertEquals(mappedEntity, mappedEntity.getCompetencyLevels().get(0).getMasterEntity(),
        "competencyLevel should back-reference its parent masterEntity");
    verify(masterEntityRepository, times(1)).save(mappedEntity);
  }

  // ─── update ──────────────────────────────────────────────────────────────────

  @Test
  @DisplayName("update - should throw BAD_REQUEST when update list is empty")
  void shouldThrowWhenUpdateListIsEmpty() {
    UpdateEntityException ex = assertThrows(UpdateEntityException.class,
        () -> masterEntityService.update(List.of(), "admin"));
    assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
  }

  @Test
  @DisplayName("update - should throw BAD_REQUEST when userId is blank")
  void shouldThrowWhenUserIdIsBlankOnUpdate() {
    EntityUpdateDTO dto = new EntityUpdateDTO();
    dto.setCode("PS001");
    dto.setLanguageCode("en");

    UpdateEntityException ex = assertThrows(UpdateEntityException.class,
        () -> masterEntityService.update(List.of(dto), ""));

    assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    verify(masterEntityRepository, never()).findByCodeAndLanguageCode(anyString(), anyString());
  }

  @Test
  @DisplayName("update - should throw NOT_FOUND when entity does not exist")
  void shouldThrowWhenEntityNotFoundOnUpdate() {
    EntityUpdateDTO dto = new EntityUpdateDTO();
    dto.setCode("UNKNOWN");
    dto.setLanguageCode("en");

    when(masterEntityRepository.findByCodeAndLanguageCode("UNKNOWN", "en")).thenReturn(Optional.empty());

    UpdateEntityException ex = assertThrows(UpdateEntityException.class,
        () -> masterEntityService.update(List.of(dto), "admin"));

    assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    verify(masterEntityRepository, never()).saveAll(anyList());
  }

  @Test
  @DisplayName("update - should apply field updates, save, and upsert to ES")
  void shouldUpdateEntitySuccessfully() {
    EntityUpdateDTO dto = new EntityUpdateDTO();
    dto.setCode("PS001");
    dto.setLanguageCode("en");
    dto.setName("Updated Name");
    dto.setDescription("Updated description");

    MasterEntity existingEntity = MasterEntity.builder()
        .code("PS001").languageCode("en").entityType(EntityType.ROLE)
        .name("Old Name").description("Old description").build();

    when(masterEntityRepository.findByCodeAndLanguageCode("PS001", "en"))
        .thenReturn(Optional.of(existingEntity));
    when(masterEntityRepository.saveAll(anyList())).thenReturn(List.of(existingEntity));
    when(masterEntityMapper.toResponseDTO(existingEntity)).thenReturn(new EntityResponseDTO());

    AppResponse result = masterEntityService.update(List.of(dto), "admin");

    assertNotNull(result);
    assertEquals(HttpStatus.OK.getReasonPhrase(), result.getResponseCode());
    assertEquals("Updated Name", existingEntity.getName());
    assertEquals("Updated description", existingEntity.getDescription());
    assertEquals("admin", existingEntity.getUpdatedBy());
    assertNotNull(existingEntity.getUpdatedAt());
    verify(masterEntityRepository, times(1)).saveAll(List.of(existingEntity));
    verify(elasticSearchEntityRepository, times(1)).save(any(MasterEntityDocument.class));
  }

  // ─── deleteMasterEntities ────────────────────────────────────────────────────

  @Test
  @DisplayName("deleteMasterEntities - should throw BAD_REQUEST when delete list is empty")
  void shouldThrowWhenDeleteListIsEmpty() {
    UpdateEntityException ex = assertThrows(UpdateEntityException.class,
        () -> masterEntityService.deleteMasterEntities(List.of()));
    assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
  }

  @Test
  @DisplayName("deleteMasterEntities - should throw BAD_REQUEST when neither language nor purgeAllLanguage is provided")
  void shouldThrowWhenNeitherLanguageNorPurgeProvided() {
    EntityDeleteRequestDTO dto = new EntityDeleteRequestDTO();
    dto.setEntityCode("R001");
    dto.setEntityType(EntityType.ROLE);
    // language is null, purgeAllLanguage defaults to false

    UpdateEntityException ex = assertThrows(UpdateEntityException.class,
        () -> masterEntityService.deleteMasterEntities(List.of(dto)));

    assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    verify(masterEntityRepository, never()).delete(any());
  }

  @Test
  @DisplayName("deleteMasterEntities - should throw NOT_FOUND when entity not found for single language delete")
  void shouldThrowWhenEntityNotFoundOnSingleDelete() {
    EntityDeleteRequestDTO dto = new EntityDeleteRequestDTO();
    dto.setEntityCode("R001");
    dto.setEntityType(EntityType.ROLE);
    dto.setLanguage("en");

    when(masterEntityRepository.findByCodeAndLanguageCode("R001", "en")).thenReturn(Optional.empty());

    UpdateEntityException ex = assertThrows(UpdateEntityException.class,
        () -> masterEntityService.deleteMasterEntities(List.of(dto)));

    assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    verify(masterEntityRepository, never()).delete(any(MasterEntity.class));
  }

  @Test
  @DisplayName("deleteMasterEntities - should delete entity and its mappings when it is the last language variant")
  void shouldDeleteEntityAndMappingsWhenLastVariant() {
    EntityDeleteRequestDTO dto = new EntityDeleteRequestDTO();
    dto.setEntityCode("R001");
    dto.setEntityType(EntityType.ROLE);
    dto.setLanguage("en");

    MasterEntity entity = MasterEntity.builder()
        .code("R001").languageCode("en").entityType(EntityType.ROLE).build();

    when(masterEntityRepository.findByCodeAndLanguageCode("R001", "en")).thenReturn(Optional.of(entity));
    when(masterEntityRepository.findByCodeAndEntityType("R001", EntityType.ROLE)).thenReturn(List.of(entity));

    AppResponse result = masterEntityService.deleteMasterEntities(List.of(dto));

    assertNotNull(result);
    assertEquals(HttpStatus.OK.getReasonPhrase(), result.getResponseCode());
    verify(entityMapRepository, times(1)).deleteByParentEntityCodeAndParentEntityType("R001", EntityType.ROLE);
    verify(entityMapRepository, times(1)).deleteByChildEntityCodeAndChildEntityType("R001", EntityType.ROLE);
    verify(masterEntityRepository, times(1)).delete(entity);
    verify(elasticSearchEntityRepository, times(1)).deleteById("R001_en");
  }

  @Test
  @DisplayName("deleteMasterEntities - should preserve mappings when other language variants exist")
  void shouldDeleteEntityButPreserveMappingsWhenOtherVariantsExist() {
    EntityDeleteRequestDTO dto = new EntityDeleteRequestDTO();
    dto.setEntityCode("R001");
    dto.setEntityType(EntityType.ROLE);
    dto.setLanguage("fr");

    MasterEntity frEntity = MasterEntity.builder()
        .code("R001").languageCode("fr").entityType(EntityType.ROLE).build();
    MasterEntity enEntity = MasterEntity.builder()
        .code("R001").languageCode("en").entityType(EntityType.ROLE).build();

    when(masterEntityRepository.findByCodeAndLanguageCode("R001", "fr")).thenReturn(Optional.of(frEntity));
    when(masterEntityRepository.findByCodeAndEntityType("R001", EntityType.ROLE))
        .thenReturn(List.of(frEntity, enEntity));

    AppResponse result = masterEntityService.deleteMasterEntities(List.of(dto));

    assertNotNull(result);
    verify(entityMapRepository, never()).deleteByParentEntityCodeAndParentEntityType(anyString(), any(EntityType.class));
    verify(entityMapRepository, never()).deleteByChildEntityCodeAndChildEntityType(anyString(), any(EntityType.class));
    verify(masterEntityRepository, times(1)).delete(frEntity);
    verify(elasticSearchEntityRepository, times(1)).deleteById("R001_fr");
  }

  @Test
  @DisplayName("deleteMasterEntities - should throw NOT_FOUND when entity not found during purge")
  void shouldThrowWhenEntityNotFoundOnPurge() {
    EntityDeleteRequestDTO dto = new EntityDeleteRequestDTO();
    dto.setEntityCode("R999");
    dto.setEntityType(EntityType.ROLE);
    dto.setPurgeAllLanguage(true);

    when(masterEntityRepository.findByCodeAndEntityType("R999", EntityType.ROLE)).thenReturn(List.of());

    UpdateEntityException ex = assertThrows(UpdateEntityException.class,
        () -> masterEntityService.deleteMasterEntities(List.of(dto)));

    assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    verify(masterEntityRepository, never()).deleteAll(anyList());
  }

  @Test
  @DisplayName("deleteMasterEntities - should purge all language variants and their mappings")
  void shouldPurgeAllLanguageVariants() {
    EntityDeleteRequestDTO dto = new EntityDeleteRequestDTO();
    dto.setEntityCode("R001");
    dto.setEntityType(EntityType.ROLE);
    dto.setPurgeAllLanguage(true);

    MasterEntity enEntity = MasterEntity.builder()
        .code("R001").languageCode("en").entityType(EntityType.ROLE).build();
    MasterEntity frEntity = MasterEntity.builder()
        .code("R001").languageCode("fr").entityType(EntityType.ROLE).build();
    List<MasterEntity> allVariants = List.of(enEntity, frEntity);

    when(masterEntityRepository.findByCodeAndEntityType("R001", EntityType.ROLE)).thenReturn(allVariants);

    AppResponse result = masterEntityService.deleteMasterEntities(List.of(dto));

    assertNotNull(result);
    assertEquals(HttpStatus.OK.getReasonPhrase(), result.getResponseCode());
    verify(entityMapRepository, times(1)).deleteByParentEntityCodeAndParentEntityType("R001", EntityType.ROLE);
    verify(entityMapRepository, times(1)).deleteByChildEntityCodeAndChildEntityType("R001", EntityType.ROLE);
    verify(masterEntityRepository, times(1)).deleteAll(allVariants);
    verify(elasticSearchEntityRepository, times(1)).deleteById("R001_en");
    verify(elasticSearchEntityRepository, times(1)).deleteById("R001_fr");
  }
}
