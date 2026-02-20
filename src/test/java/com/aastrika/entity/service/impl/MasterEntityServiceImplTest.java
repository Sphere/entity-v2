package com.aastrika.entity.service.impl;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aastrika.entity.document.MasterEntityDocument;
import com.aastrika.entity.dto.request.EntityCreateRequestDTO;
import com.aastrika.entity.dto.response.AppResponse;
import com.aastrika.entity.exception.UpdateEntityException;
import com.aastrika.entity.mapper.MasterEntityMapper;
import com.aastrika.entity.model.MasterEntity;
import com.aastrika.entity.reader.EntitySheetReaderFactory;
import com.aastrika.entity.repository.es.ElasticSearchEntityRepository;
import com.aastrika.entity.repository.jpa.MasterEntityRepository;
import com.aastrika.entity.service.MasterEntityEsService;
import com.aastrika.entity.util.EntityUtil;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

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

  @Test
  @DisplayName("create - should save entity and index to ES successfully")
  void shouldCreateEntitySuccessfully() {
    // Arrange
    EntityCreateRequestDTO requestDTO = new EntityCreateRequestDTO();
    requestDTO.setCode("PS001");
    requestDTO.setLanguageCode("en");
    requestDTO.setEntityType("ROLE");
    requestDTO.setType("Role");
    requestDTO.setName("Problem Solving");
    requestDTO.setDescription("Ability to solve problems");
    requestDTO.setStatus("Active");

    MasterEntity mappedEntity = MasterEntity.builder()
        .name("Problem Solving")
        .type("Role")
        .entityType("ROLE")
        .description("Ability to solve problems")
        .code("PS001")
        .languageCode("en")
        .status("Active")
        .build();

    when(masterEntityRepository.findByCodeAndLanguageCode("PS001", "en"))
        .thenReturn(Optional.empty());
    when(masterEntityMapper.toEntity(any(EntityCreateRequestDTO.class)))
        .thenReturn(mappedEntity);
    when(masterEntityRepository.save(any(MasterEntity.class)))
        .thenReturn(mappedEntity);

    // Act
    AppResponse result = masterEntityService.create(requestDTO, "admin");

    // Assert
    assertAll(
        () -> assertNotNull(result),
        () -> assertEquals(HttpStatus.OK.getReasonPhrase(), result.getResponseCode())
    );

    // Verify createdAt and createdBy were set
    assertNotNull(mappedEntity.getCreatedAt(), "createdAt should be set before save");
    assertEquals("admin", mappedEntity.getCreatedBy(), "createdBy should be set to userId");

    // Verify DB save was called
    verify(masterEntityRepository, times(1)).save(mappedEntity);

    // Verify ES indexing happened
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
  @DisplayName("create - should throw exception when entity already exists")
  void shouldThrowExceptionWhenEntityAlreadyExists() {
    // Arrange
    EntityCreateRequestDTO requestDTO = new EntityCreateRequestDTO();
    requestDTO.setCode("PS001");
    requestDTO.setLanguageCode("en");

    MasterEntity existingEntity = MasterEntity.builder()
        .id(1)
        .code("PS001")
        .languageCode("en")
        .build();

    when(masterEntityRepository.findByCodeAndLanguageCode("PS001", "en"))
        .thenReturn(Optional.of(existingEntity));

    // Act & Assert
    UpdateEntityException exception = assertThrows(UpdateEntityException.class,
        () -> masterEntityService.create(requestDTO, "admin"));

    assertEquals(HttpStatus.CONFLICT, exception.getStatus());

    // Verify save was never called
    verify(masterEntityRepository, never()).save(any(MasterEntity.class));
    verify(elasticSearchEntityRepository, never()).save(any(MasterEntityDocument.class));
  }

  @Test
  @DisplayName("create - should throw exception when userId is blank")
  void shouldThrowExceptionWhenUserIdIsBlank() {
    // Arrange
    EntityCreateRequestDTO requestDTO = new EntityCreateRequestDTO();
    requestDTO.setCode("PS001");
    requestDTO.setLanguageCode("en");

    // Act & Assert
    UpdateEntityException exception = assertThrows(UpdateEntityException.class,
        () -> masterEntityService.create(requestDTO, ""));

    assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());

    // Verify no DB or ES operations
    verify(masterEntityRepository, never()).findByCodeAndLanguageCode(anyString(), anyString());
    verify(masterEntityRepository, never()).save(any(MasterEntity.class));
  }
}