package com.aastrika.entity.service.impl;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aastrika.entity.document.MasterEntityDocument;
import com.aastrika.entity.dto.request.SearchDTO;
import com.aastrika.entity.mapper.MasterEntityMapper;
import com.aastrika.entity.repository.es.ElasticSearchEntityRepository;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;

@ExtendWith(MockitoExtension.class)
class MasterEntityEsServiceImplTest {

  @Mock
  private ElasticSearchEntityRepository elasticSearchEntityRepository;

  @Mock
  private MasterEntityMapper masterEntityMapper;

  @Mock
  private ElasticsearchOperations elasticsearchOperations;

  @Mock
  private SearchHits<MasterEntityDocument> searchHits;

  @Mock
  private SearchHit<MasterEntityDocument> searchHit;

  @InjectMocks
  private MasterEntityEsServiceImpl masterEntityEsService;

  @Test
  @DisplayName("findEntitiesBySearchParameter - fuzzy search (strict=false) returns matching documents")
  void shouldReturnDocumentsForFuzzySearch() {
    // Arrange
    SearchDTO searchDTO = new SearchDTO();
    searchDTO.setEntityType("COMPETENCY");
    searchDTO.setLanguage("en");
    searchDTO.setQuery("problem solving");
    searchDTO.setStrict(false);
    searchDTO.setField(List.of("name", "description"));

    MasterEntityDocument expectedDoc = MasterEntityDocument.builder()
        .id("1")
        .entityType("COMPETENCY")
        .name("Problem Solving")
        .description("Ability to solve problems")
        .code("PS001")
        .languageCode("en")
        .status("Active")
        .build();

    when(searchHit.getContent()).thenReturn(expectedDoc);
    when(searchHits.getSearchHits()).thenReturn(List.of(searchHit));
    when(elasticsearchOperations.search(any(NativeQuery.class), eq(MasterEntityDocument.class)))
        .thenReturn(searchHits);

    // Act
    List<MasterEntityDocument> results = masterEntityEsService.findEntitiesBySearchParameter(searchDTO);

    // Assert
    assertAll(
        () -> assertNotNull(results, "Results should not be null"),
        () -> assertEquals(1, results.size(), "Should return 1 document"),
        () -> assertEquals("Problem Solving", results.get(0).getName()),
        () -> assertEquals("PS001", results.get(0).getCode()),
        () -> assertEquals("en", results.get(0).getLanguageCode())
    );

    verify(elasticsearchOperations, times(1))
        .search(any(NativeQuery.class), eq(MasterEntityDocument.class));
  }

  @Test
  @DisplayName("findEntitiesBySearchParameter - phrase search (strict=true) returns matching documents")
  void shouldReturnDocumentsForPhraseSearch() {
    // Arrange
    SearchDTO searchDTO = new SearchDTO();
    searchDTO.setEntityType("COMPETENCY");
    searchDTO.setLanguage("en");
    searchDTO.setQuery("communication skills");
    searchDTO.setStrict(true);
    searchDTO.setField(List.of("name"));

    MasterEntityDocument expectedDoc = MasterEntityDocument.builder()
        .id("2")
        .entityType("COMPETENCY")
        .name("Communication Skills")
        .description("Effective communication")
        .code("CS001")
        .languageCode("en")
        .status("Active")
        .build();

    when(searchHit.getContent()).thenReturn(expectedDoc);
    when(searchHits.getSearchHits()).thenReturn(List.of(searchHit));
    when(elasticsearchOperations.search(any(NativeQuery.class), eq(MasterEntityDocument.class)))
        .thenReturn(searchHits);

    // Act
    List<MasterEntityDocument> results = masterEntityEsService.findEntitiesBySearchParameter(searchDTO);

    // Assert
    assertAll(
        () -> assertNotNull(results),
        () -> assertEquals(1, results.size()),
        () -> assertEquals("Communication Skills", results.get(0).getName()),
        () -> assertEquals("CS001", results.get(0).getCode())
    );

    verify(elasticsearchOperations, times(1))
        .search(any(NativeQuery.class), eq(MasterEntityDocument.class));
  }

  @Test
  @DisplayName("findEntitiesBySearchParameter - returns empty list when no match found")
  void shouldReturnEmptyListWhenNoMatch() {
    // Arrange
    SearchDTO searchDTO = new SearchDTO();
    searchDTO.setEntityType("COMPETENCY");
    searchDTO.setLanguage("en");
    searchDTO.setQuery("nonexistent");
    searchDTO.setStrict(false);
    searchDTO.setField(List.of("name"));

    when(searchHits.getSearchHits()).thenReturn(List.of());
    when(elasticsearchOperations.search(any(NativeQuery.class), eq(MasterEntityDocument.class)))
        .thenReturn(searchHits);

    // Act
    List<MasterEntityDocument> results = masterEntityEsService.findEntitiesBySearchParameter(searchDTO);

    // Assert
    assertAll(
        () -> assertNotNull(results, "Results should not be null even when empty"),
        () -> assertTrue(results.isEmpty(), "Should return empty list")
    );

    verify(elasticsearchOperations, times(1))
        .search(any(NativeQuery.class), eq(MasterEntityDocument.class));
  }
}
