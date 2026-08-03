package com.aastrika.entity.service.impl;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aastrika.entity.document.MasterEntityDocument;
import com.aastrika.entity.dto.EntitySheetRow;
import com.aastrika.entity.dto.request.SearchDTO;
import com.aastrika.entity.enums.EntityType;
import com.aastrika.entity.dto.response.AppResponse;
import com.aastrika.entity.dto.response.EntityResult;
import com.aastrika.entity.dto.response.MasterEntitySearchResponseDTO;
import com.aastrika.entity.mapper.MasterEntityMapper;
import com.aastrika.entity.repository.es.ElasticSearchEntityRepository;
import com.aastrika.entity.support.EntityTypeExtension;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.mockito.ArgumentCaptor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opensearch.common.unit.Fuzziness;
import org.opensearch.data.client.orhlc.NativeSearchQuery;
import org.opensearch.data.core.OpenSearchOperations;
import org.opensearch.index.query.BoolQueryBuilder;
import org.opensearch.index.query.MatchPhraseQueryBuilder;
import org.opensearch.index.query.MatchQueryBuilder;
import org.opensearch.index.query.QueryBuilder;
import org.opensearch.index.query.TermQueryBuilder;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;

@ExtendWith(MockitoExtension.class)
@ExtendWith(EntityTypeExtension.class)
class MasterEntityEsServiceImplTest {

  @Mock
  private ElasticSearchEntityRepository elasticSearchEntityRepository;

  @Mock
  private MasterEntityMapper masterEntityMapper;

  @Mock
  private OpenSearchOperations openSearchOperations;

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
    searchDTO.setEntityType(EntityType.COMPETENCY);
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

    MasterEntitySearchResponseDTO responseDTO = MasterEntitySearchResponseDTO.builder()
        .name("Problem Solving").code("PS001").languageCode("en").build();

    when(searchHit.getContent()).thenReturn(expectedDoc);
    when(searchHits.getSearchHits()).thenReturn(List.of(searchHit));
    when(openSearchOperations.search(any(NativeSearchQuery.class), eq(MasterEntityDocument.class)))
        .thenReturn(searchHits);
    when(masterEntityMapper.toSearchResponse(expectedDoc)).thenReturn(responseDTO);

    // Act
    AppResponse<EntityResult<MasterEntitySearchResponseDTO>> response =
        masterEntityEsService.findEntitiesBySearchParameter(searchDTO);

    // Assert
    EntityResult<MasterEntitySearchResponseDTO> result = response.getResult();
    assertAll(
        () -> assertNotNull(result, "Result should not be null"),
        () -> assertEquals(1, result.getCount(), "Count should be 1"),
        () -> assertEquals("Problem Solving", result.getEntity().get(0).getName()),
        () -> assertEquals("PS001", result.getEntity().get(0).getCode()),
        () -> assertEquals("en", result.getEntity().get(0).getLanguageCode())
    );

    // Assert the query actually handed to OpenSearch. Without this the test passes
    // regardless of which clauses were built.
    BoolQueryBuilder boolQuery = capturedBoolQuery();

    assertEquals(2, boolQuery.must().size(), "must → entityType filter + languageCode filter");
    MatchQueryBuilder typeClause = assertInstanceOf(MatchQueryBuilder.class, boolQuery.must().get(0));
    TermQueryBuilder languageClause = assertInstanceOf(TermQueryBuilder.class, boolQuery.must().get(1));
    assertAll(
        () -> assertEquals("entityType", typeClause.fieldName()),
        () -> assertEquals(EntityType.COMPETENCY, typeClause.value()),
        () -> assertEquals(Fuzziness.AUTO, typeClause.fuzziness()),
        () -> assertEquals("languageCode", languageClause.fieldName()),
        () -> assertEquals("en", languageClause.value())
    );

    assertEquals(2, boolQuery.should().size(), "one should clause per requested field");
    List<String> fuzzyFields = new ArrayList<>();
    for (QueryBuilder clause : boolQuery.should()) {
      MatchQueryBuilder fieldClause = assertInstanceOf(MatchQueryBuilder.class, clause,
          "strict=false must use fuzzy match, not match_phrase");
      assertEquals(Fuzziness.AUTO, fieldClause.fuzziness());
      fuzzyFields.add(fieldClause.fieldName());
    }
    assertEquals(List.of("name", "description"), fuzzyFields);
    assertEquals("1", boolQuery.minimumShouldMatch());
  }

  @Test
  @DisplayName("findEntitiesBySearchParameter - phrase search (strict=true) returns matching documents")
  void shouldReturnDocumentsForPhraseSearch() {
    // Arrange
    SearchDTO searchDTO = new SearchDTO();
    searchDTO.setEntityType(EntityType.COMPETENCY);
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

    MasterEntitySearchResponseDTO responseDTO = MasterEntitySearchResponseDTO.builder()
        .name("Communication Skills").code("CS001").build();

    when(searchHit.getContent()).thenReturn(expectedDoc);
    when(searchHits.getSearchHits()).thenReturn(List.of(searchHit));
    when(openSearchOperations.search(any(NativeSearchQuery.class), eq(MasterEntityDocument.class)))
        .thenReturn(searchHits);
    when(masterEntityMapper.toSearchResponse(expectedDoc)).thenReturn(responseDTO);

    // Act
    AppResponse<EntityResult<MasterEntitySearchResponseDTO>> response =
        masterEntityEsService.findEntitiesBySearchParameter(searchDTO);

    // Assert
    EntityResult<MasterEntitySearchResponseDTO> result = response.getResult();
    assertAll(
        () -> assertNotNull(result),
        () -> assertEquals(1, result.getCount()),
        () -> assertEquals("Communication Skills", result.getEntity().get(0).getName()),
        () -> assertEquals("CS001", result.getEntity().get(0).getCode())
    );

    // strict=true → match_phrase clauses. This is the only behavioural difference
    // from the fuzzy search above, so it is the one thing this test must pin down.
    BoolQueryBuilder boolQuery = capturedBoolQuery();
    assertEquals(1, boolQuery.should().size());
    MatchPhraseQueryBuilder phraseClause = assertInstanceOf(
        MatchPhraseQueryBuilder.class, boolQuery.should().get(0),
        "strict=true must use match_phrase, not fuzzy match");
    assertAll(
        () -> assertEquals("name", phraseClause.fieldName()),
        () -> assertEquals("communication skills", phraseClause.value()),
        () -> assertEquals("1", boolQuery.minimumShouldMatch())
    );
  }

  @Test
  @DisplayName("findEntitiesBySearchParameter - returns empty list when no match found")
  void shouldReturnEmptyListWhenNoMatch() {
    // Arrange
    SearchDTO searchDTO = new SearchDTO();
    searchDTO.setEntityType(EntityType.COMPETENCY);
    searchDTO.setLanguage("en");
    searchDTO.setQuery("nonexistent");
    searchDTO.setStrict(false);
    searchDTO.setField(List.of("name"));

    when(searchHits.getSearchHits()).thenReturn(List.of());
    when(openSearchOperations.search(any(NativeSearchQuery.class), eq(MasterEntityDocument.class)))
        .thenReturn(searchHits);

    // Act
    AppResponse<EntityResult<MasterEntitySearchResponseDTO>> response =
        masterEntityEsService.findEntitiesBySearchParameter(searchDTO);

    // Assert
    EntityResult<MasterEntitySearchResponseDTO> result = response.getResult();
    assertAll(
        () -> assertNotNull(result, "Result should not be null even when empty"),
        () -> assertEquals(0, result.getCount(), "Count should be 0"),
        () -> assertTrue(result.getEntity().isEmpty(), "Entity list should be empty")
    );

    verify(openSearchOperations, times(1))
        .search(any(NativeSearchQuery.class), eq(MasterEntityDocument.class));
  }

  // ─── saveEntityDetailsInES ───────────────────────────────────────────────────

  @Test
  @DisplayName("saveEntityDetailsInES - should map rows to documents, set IDs, and save all to ES")
  void shouldSaveEntityDetailsToElasticsearch() {
    EntitySheetRow row1 = EntitySheetRow.builder().code("C001").language("en").name("Communication").build();
    EntitySheetRow row2 = EntitySheetRow.builder().code("C002").language("en").name("Leadership").build();

    MasterEntityDocument doc1 = MasterEntityDocument.builder().code("C001").languageCode("en").name("Communication").build();
    MasterEntityDocument doc2 = MasterEntityDocument.builder().code("C002").languageCode("en").name("Leadership").build();

    when(masterEntityMapper.toDocument(row1)).thenReturn(doc1);
    when(masterEntityMapper.toDocument(row2)).thenReturn(doc2);
    when(elasticSearchEntityRepository.saveAll(anyList())).thenReturn(List.of(doc1, doc2));

    masterEntityEsService.saveEntityDetailsInES(List.of(row1, row2), "COMPETENCY", "testUser");

    ArgumentCaptor<List<MasterEntityDocument>> captor = ArgumentCaptor.forClass(List.class);
    verify(elasticSearchEntityRepository, times(1)).saveAll(captor.capture());

    List<MasterEntityDocument> saved = captor.getValue();
    assertAll(
        () -> assertEquals(2, saved.size()),
        () -> assertEquals("C001_en", saved.get(0).getId(), "ID should be code_language"),
        () -> assertEquals("C002_en", saved.get(1).getId(), "ID should be code_language"),
        // The userId parameter exists so the impl can stamp createdBy/createdAt on
        // every document — assert it, otherwise the contract is unverified.
        () -> assertEquals("testUser", saved.get(0).getCreatedBy()),
        () -> assertEquals("testUser", saved.get(1).getCreatedBy()),
        () -> assertNotNull(saved.get(0).getCreatedAt(), "createdAt should be stamped"),
        () -> assertNotNull(saved.get(1).getCreatedAt(), "createdAt should be stamped")
    );
  }

  // ─── phraseSearchByName ──────────────────────────────────────────────────────

  @Test
  @DisplayName("phraseSearchByName - should return matching documents")
  void shouldReturnDocumentsForPhraseSearchByName() {
    MasterEntityDocument doc = MasterEntityDocument.builder()
        .id("C001_en").code("C001").name("Communication Skills").languageCode("en").build();

    when(searchHit.getContent()).thenReturn(doc);
    when(searchHits.getSearchHits()).thenReturn(List.of(searchHit));
    when(openSearchOperations.search(any(NativeSearchQuery.class), eq(MasterEntityDocument.class)))
        .thenReturn(searchHits);

    List<MasterEntityDocument> results = masterEntityEsService.phraseSearchByName("Communication Skills");

    assertAll(
        () -> assertNotNull(results),
        () -> assertEquals(1, results.size()),
        () -> assertEquals("C001", results.get(0).getCode()),
        () -> assertEquals("Communication Skills", results.get(0).getName())
    );
    verify(openSearchOperations, times(1)).search(any(NativeSearchQuery.class), eq(MasterEntityDocument.class));
  }

  @Test
  @DisplayName("phraseSearchByName - should return empty list when no phrase match found")
  void shouldReturnEmptyListWhenNoPhraseMatch() {
    when(searchHits.getSearchHits()).thenReturn(List.of());
    when(openSearchOperations.search(any(NativeSearchQuery.class), eq(MasterEntityDocument.class)))
        .thenReturn(searchHits);

    List<MasterEntityDocument> results = masterEntityEsService.phraseSearchByName("nonexistent phrase");

    assertNotNull(results);
    assertTrue(results.isEmpty());
  }

  // ─── fuzzyPhraseSearchByName ─────────────────────────────────────────────────

  @Test
  @DisplayName("fuzzyPhraseSearchByName - should return matching documents with typo tolerance")
  void shouldReturnDocumentsForFuzzyPhraseSearchByName() {
    MasterEntityDocument doc = MasterEntityDocument.builder()
        .id("PS001_en").code("PS001").name("Problem Solving").languageCode("en").build();

    when(searchHit.getContent()).thenReturn(doc);
    when(searchHits.getSearchHits()).thenReturn(List.of(searchHit));
    when(openSearchOperations.search(any(NativeSearchQuery.class), eq(MasterEntityDocument.class)))
        .thenReturn(searchHits);

    List<MasterEntityDocument> results = masterEntityEsService.fuzzyPhraseSearchByName("Problm Solvng");

    assertAll(
        () -> assertNotNull(results),
        () -> assertEquals(1, results.size()),
        () -> assertEquals("PS001", results.get(0).getCode()),
        () -> assertEquals("Problem Solving", results.get(0).getName())
    );
    verify(openSearchOperations, times(1)).search(any(NativeSearchQuery.class), eq(MasterEntityDocument.class));
  }

  @Test
  @DisplayName("fuzzyPhraseSearchByName - should return empty list when no match found")
  void shouldReturnEmptyListWhenNoFuzzyPhraseMatch() {
    when(searchHits.getSearchHits()).thenReturn(List.of());
    when(openSearchOperations.search(any(NativeSearchQuery.class), eq(MasterEntityDocument.class)))
        .thenReturn(searchHits);

    List<MasterEntityDocument> results = masterEntityEsService.fuzzyPhraseSearchByName("xyzxyz");

    assertNotNull(results);
    assertTrue(results.isEmpty());
  }

  // ─── findEntitiesBySearchParameter (missing branches) ────────────────────────

  @Test
  @DisplayName("findEntitiesBySearchParameter - should match all entities when query is blank")
  void shouldReturnAllEntitiesWhenQueryIsBlank() {
    SearchDTO searchDTO = new SearchDTO();
    searchDTO.setEntityType(EntityType.COMPETENCY);
    searchDTO.setLanguage("en");
    searchDTO.setQuery("");
    searchDTO.setField(List.of("name"));

    MasterEntityDocument doc = MasterEntityDocument.builder()
        .id("C001_en").code("C001").name("Communication").languageCode("en").build();
    MasterEntitySearchResponseDTO responseDTO = MasterEntitySearchResponseDTO.builder()
        .name("Communication").code("C001").languageCode("en").build();

    when(searchHit.getContent()).thenReturn(doc);
    when(searchHits.getSearchHits()).thenReturn(List.of(searchHit));
    when(openSearchOperations.search(any(NativeSearchQuery.class), eq(MasterEntityDocument.class)))
        .thenReturn(searchHits);
    when(masterEntityMapper.toSearchResponse(doc)).thenReturn(responseDTO);

    AppResponse<EntityResult<MasterEntitySearchResponseDTO>> response =
        masterEntityEsService.findEntitiesBySearchParameter(searchDTO);

    EntityResult<MasterEntitySearchResponseDTO> result = response.getResult();
    assertAll(
        () -> assertNotNull(result),
        () -> assertEquals(1, result.getCount()),
        () -> assertEquals("C001", result.getEntity().get(0).getCode())
    );

    // Blank query → no should clauses at all, so everything matching the
    // entityType + languageCode filters comes back.
    BoolQueryBuilder boolQuery = capturedBoolQuery();
    assertAll(
        () -> assertEquals(2, boolQuery.must().size(), "entityType + languageCode filters remain"),
        () -> assertTrue(boolQuery.should().isEmpty(), "blank query must not add should clauses"),
        () -> assertNull(boolQuery.minimumShouldMatch(),
            "minimumShouldMatch only applies when should clauses exist")
    );
  }

  @Test
  @DisplayName("findEntitiesBySearchParameter - should search without language filter when language is blank")
  void shouldSearchWithoutLanguageFilterWhenLanguageIsBlank() {
    SearchDTO searchDTO = new SearchDTO();
    searchDTO.setEntityType("ROLE");
    searchDTO.setLanguage("");
    searchDTO.setQuery("developer");
    searchDTO.setStrict(false);
    searchDTO.setField(List.of("name"));

    MasterEntityDocument doc = MasterEntityDocument.builder()
        .id("R001_en").code("R001").name("Developer").languageCode("en").build();
    MasterEntitySearchResponseDTO responseDTO = MasterEntitySearchResponseDTO.builder()
        .name("Developer").code("R001").build();

    when(searchHit.getContent()).thenReturn(doc);
    when(searchHits.getSearchHits()).thenReturn(List.of(searchHit));
    when(openSearchOperations.search(any(NativeSearchQuery.class), eq(MasterEntityDocument.class)))
        .thenReturn(searchHits);
    when(masterEntityMapper.toSearchResponse(doc)).thenReturn(responseDTO);

    AppResponse<EntityResult<MasterEntitySearchResponseDTO>> response =
        masterEntityEsService.findEntitiesBySearchParameter(searchDTO);

    EntityResult<MasterEntitySearchResponseDTO> result = response.getResult();
    assertAll(
        () -> assertNotNull(result),
        () -> assertEquals(1, result.getCount()),
        () -> assertEquals("R001", result.getEntity().get(0).getCode())
    );

    // Blank language → the languageCode term filter must be omitted entirely.
    BoolQueryBuilder boolQuery = capturedBoolQuery();
    assertEquals(1, boolQuery.must().size(), "only the entityType filter should remain");
    MatchQueryBuilder typeClause = assertInstanceOf(MatchQueryBuilder.class, boolQuery.must().get(0));
    assertAll(
        () -> assertEquals("entityType", typeClause.fieldName()),
        () -> assertEquals("ROLE", typeClause.value()),
        () -> assertTrue(boolQuery.must().stream().noneMatch(TermQueryBuilder.class::isInstance),
            "no languageCode term filter should be added when language is blank")
    );
  }

  // ─── Query capture helpers ───────────────────────────────────────────────────

  /** Mirrors MasterEntityEsServiceImpl.DEFAULT_PAGE_SIZE, which is private. */
  private static final int DEFAULT_PAGE_SIZE = 500;

  /**
   * Captures the query handed to OpenSearch and asserts the page-size cap.
   *
   * <p>Replaces {@code verify(...).search(any(NativeSearchQuery.class), ...)} — {@code any()}
   * discarded the one object these tests exist to verify, so every branch of
   * findEntitiesBySearchParameter produced an identical, unverifiable assertion.
   */
  private NativeSearchQuery capturedQuery() {
    ArgumentCaptor<NativeSearchQuery> captor = ArgumentCaptor.forClass(NativeSearchQuery.class);
    verify(openSearchOperations).search(captor.capture(), eq(MasterEntityDocument.class));
    NativeSearchQuery query = captor.getValue();
    assertEquals(DEFAULT_PAGE_SIZE, query.getMaxResults().intValue(),
        "search must be capped at the default page size");
    return query;
  }

  private BoolQueryBuilder capturedBoolQuery() {
    return assertInstanceOf(BoolQueryBuilder.class, capturedQuery().getQuery());
  }
}
