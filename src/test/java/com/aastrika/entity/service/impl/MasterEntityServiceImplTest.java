package com.aastrika.entity.service.impl;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aastrika.entity.document.MasterEntityDocument;
import com.aastrika.entity.mapper.MasterEntityMapper;
import com.aastrika.entity.model.MasterEntity;
import com.aastrika.entity.reader.EntitySheetReaderFactory;
import com.aastrika.entity.repository.es.ElasticSearchEntityRepository;
import com.aastrika.entity.repository.jpa.MasterEntityRepository;
import com.aastrika.entity.service.MasterEntityEsService;
import com.aastrika.entity.util.EntityUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MasterEntityServiceImplTest {

  @Mock
  private MasterEntityRepository masterEntityRepository;

  @Mock
  private ElasticSearchEntityRepository elasticSearchEntityRepository;

  @Mock
  private MasterEntityMapper masterEntityMapper;

  @Mock
  private EntitySheetReaderFactory entitySheetReaderFactory;

  @Mock
  private EntityUtil entityUtil;

  @Mock
  private MasterEntityEsService masterEntityEsService;

  @InjectMocks
  private MasterEntityServiceImpl masterEntityService;

  // ---------------------------------------------------------------------------
  // Approach 1: thenReturn
  // Use when: You only need to verify the service logic and interactions.
  // Mock returns a predefined object. Assertions are on the canned response.
  // Limitation: result is always the savedEntity object, not the actual input.
  // ---------------------------------------------------------------------------
  @Test
  @DisplayName("create - thenReturn: verify save and ES indexing")
  void shouldCreateEntity_thenReturn() {
    // Arrange
    MasterEntity inputEntity = MasterEntity.builder()
        .name("Problem Solving")
        .type("Competency")
        .entityType("COMPETENCY")
        .description("Ability to solve problems")
        .code("PS001")
        .languageCode("en")
        .status("Active")
        .createdBy("admin")
        .build();

    MasterEntity savedEntity = MasterEntity.builder()
        .id(1)
        .name("Problem Solving")
        .type("Competency")
        .entityType("COMPETENCY")
        .description("Ability to solve problems")
        .code("PS001")
        .languageCode("en")
        .status("Active")
        .createdBy("admin")
        .build();

    when(masterEntityRepository.save(any(MasterEntity.class))).thenReturn(savedEntity);

    // Act
    MasterEntity result = masterEntityService.create(inputEntity);

    // Assert
    assertAll(
        () -> assertNotNull(result, "Returned entity should not be null"),
        () -> assertEquals(1, result.getId(), "ID should be set after save"),
        () -> assertEquals("Problem Solving", result.getName()),
        () -> assertEquals("PS001", result.getCode())
    );

    // Verify createdAt was set on input before save
    assertNotNull(inputEntity.getCreatedAt(), "createdAt should be set on input before save");

    // Verify DB save was called
    verify(masterEntityRepository, times(1)).save(inputEntity);

    // Verify Elasticsearch indexing happened
    ArgumentCaptor<MasterEntityDocument> esCaptor = ArgumentCaptor.forClass(MasterEntityDocument.class);
    verify(elasticSearchEntityRepository, times(1)).save(esCaptor.capture());

    MasterEntityDocument indexedDoc = esCaptor.getValue();
    assertAll(
        () -> assertEquals("Competency", indexedDoc.getType()),
        () -> assertEquals("Problem Solving", indexedDoc.getName()),
        () -> assertEquals("Ability to solve problems", indexedDoc.getDescription()),
        () -> assertEquals("Active", indexedDoc.getStatus()),
        () -> assertEquals("admin", indexedDoc.getCreatedBy())
    );
  }

  // ---------------------------------------------------------------------------
  // Approach 2: thenAnswer
  // Use when: You want the mock to behave like real JPA — return the same
  // object that was passed in (with ID set). This way, changing inputEntity
  // data automatically reflects in the result. No separate savedEntity needed.
  // ---------------------------------------------------------------------------
  @Test
  @DisplayName("create - thenAnswer: verify input data flows through correctly")
  void shouldCreateEntity_thenAnswer() {
    // Arrange
    MasterEntity inputEntity = MasterEntity.builder()
        .name("Problem Solving")
        .type("Competency")
        .entityType("COMPETENCY")
        .description("Ability to solve problems")
        .code("PS001")
        .languageCode("en")
        .status("Active")
        .createdBy("admin")
        .build();

    when(masterEntityRepository.save(any(MasterEntity.class))).thenAnswer(invocation -> {
      MasterEntity entity = invocation.getArgument(0);
      entity.setId(1);
      return entity;
    });

    // Act
    MasterEntity result = masterEntityService.create(inputEntity);

    // Assert — result IS the inputEntity, so any mismatch will fail
    assertAll(
        () -> assertNotNull(result, "Returned entity should not be null"),
        () -> assertEquals(1, result.getId(), "ID should be set after save"),
        () -> assertNotNull(result.getCreatedAt(), "createdAt should be set before saving"),
        () -> assertEquals("Problem Solving", result.getName()),
        () -> assertEquals("PS001", result.getCode())
    );

    // Verify DB save was called
    verify(masterEntityRepository, times(1)).save(inputEntity);

    // Verify Elasticsearch indexing happened
    ArgumentCaptor<MasterEntityDocument> esCaptor = ArgumentCaptor.forClass(MasterEntityDocument.class);
    verify(elasticSearchEntityRepository, times(1)).save(esCaptor.capture());

    MasterEntityDocument indexedDoc = esCaptor.getValue();
    assertAll(
        () -> assertEquals("Competency", indexedDoc.getType()),
        () -> assertEquals("Problem Solving", indexedDoc.getName()),
        () -> assertEquals("Ability to solve problems", indexedDoc.getDescription()),
        () -> assertEquals("Active", indexedDoc.getStatus()),
        () -> assertEquals("admin", indexedDoc.getCreatedBy())
    );
  }
}
