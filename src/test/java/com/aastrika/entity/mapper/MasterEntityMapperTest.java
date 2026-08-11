package com.aastrika.entity.mapper;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.aastrika.entity.document.MasterEntityDocument;
import com.aastrika.entity.dto.EntitySheetRow;
import com.aastrika.entity.dto.request.CompetencyLevelDTO;
import com.aastrika.entity.dto.request.EntityCreateRequestDTO;
import com.aastrika.entity.dto.response.CompetencyLevelResponseDTO;
import com.aastrika.entity.dto.response.EntityResponseDTO;
import com.aastrika.entity.dto.response.MasterEntitySearchResponseDTO;
import com.aastrika.entity.model.CompetencyLevel;
import com.aastrika.entity.model.MasterEntity;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Exercises the MapStruct-generated MasterEntityMapperImpl directly.
 *
 * <p>Every service test mocks this mapper, so an incorrect or missing {@code @Mapping} is
 * invisible there. These tests assert the generated mapping field by field, including the
 * targets the compiler reports as unmapped.
 */
class MasterEntityMapperTest {

  private final MasterEntityMapper mapper = new MasterEntityMapperImpl();

  private static final Date CREATED_AT = new Date(1_700_000_000_000L);
  private static final Date UPDATED_AT = new Date(1_700_000_600_000L);

  @Test
  @DisplayName("all mappings - should return null for null input")
  void shouldReturnNullForNullInputs() {
    assertAll(
        () -> assertNull(mapper.toEntity((MasterEntityDocument) null)),
        () -> assertNull(mapper.toEntity((EntitySheetRow) null)),
        () -> assertNull(mapper.toEntity((EntityCreateRequestDTO) null)),
        () -> assertNull(mapper.toDocument(null)),
        () -> assertNull(mapper.toResponseDTO((MasterEntity) null)),
        () -> assertNull(mapper.toResponseDTO((CompetencyLevel) null)),
        () -> assertNull(mapper.toCompetencyLevel(null)),
        () -> assertNull(mapper.toSearchResponseDTO(null))
    );
  }

  // ─── toDocument(EntitySheetRow) — the sheet-upload path into OpenSearch ───────

  @Test
  @DisplayName("toDocument - should map every sheet column including all five competency levels")
  void shouldMapSheetRowToDocument() {
    EntitySheetRow row = fullSheetRow();

    MasterEntityDocument document = mapper.toDocument(row);

    assertAll(
        () -> assertEquals("C001", document.getCode()),
        () -> assertEquals("en", document.getLanguageCode(), "language → languageCode"),
        () -> assertEquals(CREATED_AT, document.getCreatedAt(), "createdDate → createdAt"),
        () -> assertEquals("E-1", document.getEntityId()),
        () -> assertEquals("COMPETENCY", document.getEntityType()),
        () -> assertEquals("Behavioural", document.getType()),
        () -> assertEquals("Clinical", document.getArea()),
        () -> assertEquals("Communication", document.getName()),
        () -> assertEquals("Communicates clearly", document.getDescription()),
        () -> assertEquals("L-9", document.getLevelId()),
        () -> assertEquals("uploader", document.getCreatedBy()),
        () -> assertEquals("editor", document.getUpdatedBy())
    );

    assertAll(
        () -> assertEquals("Basic", document.getCompetencyLevel1Name()),
        () -> assertEquals("Level 1 desc", document.getCompetencyLevel1Description()),
        () -> assertEquals("Intermediate", document.getCompetencyLevel2Name()),
        () -> assertEquals("Level 2 desc", document.getCompetencyLevel2Description()),
        () -> assertEquals("Proficient", document.getCompetencyLevel3Name()),
        () -> assertEquals("Level 3 desc", document.getCompetencyLevel3Description()),
        () -> assertEquals("Advanced", document.getCompetencyLevel4Name()),
        () -> assertEquals("Level 4 desc", document.getCompetencyLevel4Description()),
        () -> assertEquals("Expert", document.getCompetencyLevel5Name()),
        () -> assertEquals("Level 5 desc", document.getCompetencyLevel5Description())
    );

    assertAll(
        () -> assertEquals("Active", document.getStatus(),
            "status is a @Mapping constant — sheet uploads are always indexed as Active"),
        () -> assertNull(document.getId(), "id is set by MasterEntityEsServiceImpl as code_language"),
        () -> assertNull(document.getLevel(), "level is @Mapping(ignore)"),
        () -> assertNull(document.getUpdatedAt(), "updatedAt is @Mapping(ignore)")
    );
  }

  @Test
  @DisplayName("toDocument - should defensively copy additionalProperties")
  void shouldCopyAdditionalPropertiesIntoDocument() {
    Map<String, Object> source = Map.of("region", "north", "weight", 3);
    EntitySheetRow row = EntitySheetRow.builder().code("C001").additionalProperties(source).build();

    MasterEntityDocument document = mapper.toDocument(row);

    assertAll(
        () -> assertEquals(source, document.getAdditionalProperties()),
        () -> assertNotSame(source, document.getAdditionalProperties(),
            "MapStruct copies the map rather than sharing the reference")
    );
  }

  @Test
  @DisplayName("toDocument - should leave additionalProperties null when the row has none")
  void shouldLeaveAdditionalPropertiesNullOnDocument() {
    assertNull(mapper.toDocument(EntitySheetRow.builder().code("C001").build()).getAdditionalProperties());
  }

  // ─── toEntity(EntitySheetRow) — the sheet-upload path into PostgreSQL ─────────

  @Test
  @DisplayName("toEntity(EntitySheetRow) - should map sheet columns to the JPA entity")
  void shouldMapSheetRowToEntity() {
    MasterEntity entity = mapper.toEntity(fullSheetRow());

    assertAll(
        () -> assertEquals("C001", entity.getCode()),
        () -> assertEquals("en", entity.getLanguageCode(), "language → languageCode"),
        () -> assertEquals(CREATED_AT, entity.getCreatedAt(), "createdDate → createdAt"),
        () -> assertEquals("E-1", entity.getEntityId()),
        () -> assertEquals("COMPETENCY", entity.getEntityType()),
        () -> assertEquals("Behavioural", entity.getType()),
        () -> assertEquals("Clinical", entity.getArea()),
        () -> assertEquals("Communication", entity.getName()),
        () -> assertEquals("Communicates clearly", entity.getDescription()),
        () -> assertEquals("L-9", entity.getLevelId()),
        () -> assertEquals("uploader", entity.getCreatedBy()),
        () -> assertEquals("editor", entity.getUpdatedBy()),
        () -> assertEquals("reviewer", entity.getReviewedBy())
    );

    assertAll(
        () -> assertNull(entity.getId(), "id is @Mapping(ignore) — assigned by the database"),
        () -> assertNull(entity.getUpdatedAt(), "updatedAt is @Mapping(ignore)"),
        () -> assertNull(entity.getReviewedAt(), "reviewedAt is @Mapping(ignore)"),
        () -> assertNull(entity.getSource(), "source is @Mapping(ignore)"),
        () -> assertNull(entity.getLevel(), "level is @Mapping(ignore)"),
        () -> assertNull(entity.getCompetencyLevels(),
            "competencyLevels is @Mapping(ignore) — EntityUtil populates it for COMPETENCY uploads")
    );
  }

  /**
   * The compiler reports "Unmapped target property: status" for this mapping, and the
   * generated impl confirms it: no status is assigned. toDocument, by contrast, hardcodes
   * status = "Active". A single sheet upload therefore lands as Active in OpenSearch and
   * null in PostgreSQL.
   */
  @Test
  @DisplayName("toEntity(EntitySheetRow) - status is left null, unlike toDocument which sets Active")
  void shouldLeaveStatusNullOnEntityButActiveOnDocument() {
    EntitySheetRow row = fullSheetRow();

    assertAll(
        () -> assertNull(mapper.toEntity(row).getStatus(),
            "no status mapping exists for the JPA entity"),
        () -> assertEquals("Active", mapper.toDocument(row).getStatus(),
            "the OpenSearch document gets a constant Active")
    );
  }

  @Test
  @DisplayName("toEntity(EntitySheetRow) - should defensively copy additionalProperties")
  void shouldCopyAdditionalPropertiesIntoEntity() {
    Map<String, Object> source = Map.of("region", "north");
    MasterEntity entity = mapper.toEntity(
        EntitySheetRow.builder().code("C001").additionalProperties(source).build());

    assertAll(
        () -> assertEquals(source, entity.getAdditionalProperties()),
        () -> assertNotSame(source, entity.getAdditionalProperties())
    );
  }

  @Test
  @DisplayName("toEntity(EntitySheetRow) - should leave additionalProperties null when the row has none")
  void shouldLeaveAdditionalPropertiesNullOnEntity() {
    assertNull(mapper.toEntity(EntitySheetRow.builder().code("C001").build()).getAdditionalProperties());
  }

  // ─── toEntity(EntityCreateRequestDTO) — the create-API path ───────────────────

  @Test
  @DisplayName("toEntity(EntityCreateRequestDTO) - should map request fields and nested competency levels")
  void shouldMapCreateRequestToEntity() {
    EntityCreateRequestDTO request = new EntityCreateRequestDTO();
    request.setCode("C001");
    request.setLanguageCode("en");
    request.setEntityId("E-1");
    request.setEntityType("COMPETENCY");
    request.setType("Behavioural");
    request.setArea("Clinical");
    request.setName("Communication");
    request.setDescription("Communicates clearly");
    request.setStatus("Active");
    request.setLevel("L1");
    request.setLevelId("L-9");
    request.setSource("manual");
    request.setAdditionalProperties(Map.of("region", "north"));
    request.setCompetencyLevels(List.of(
        CompetencyLevelDTO.builder().levelNumber(1).levelName("Basic").levelDescription("Beginner").build(),
        CompetencyLevelDTO.builder().levelNumber(2).levelName("Advanced").levelDescription("Expert").build()));

    MasterEntity entity = mapper.toEntity(request);

    assertAll(
        () -> assertEquals("C001", entity.getCode()),
        () -> assertEquals("en", entity.getLanguageCode()),
        () -> assertEquals("E-1", entity.getEntityId()),
        () -> assertEquals("COMPETENCY", entity.getEntityType()),
        () -> assertEquals("Behavioural", entity.getType()),
        () -> assertEquals("Clinical", entity.getArea()),
        () -> assertEquals("Communication", entity.getName()),
        () -> assertEquals("Communicates clearly", entity.getDescription()),
        () -> assertEquals("Active", entity.getStatus()),
        () -> assertEquals("L1", entity.getLevel()),
        () -> assertEquals("L-9", entity.getLevelId()),
        () -> assertEquals("manual", entity.getSource()),
        () -> assertEquals(Map.of("region", "north"), entity.getAdditionalProperties())
    );

    assertAll(
        () -> assertEquals(2, entity.getCompetencyLevels().size()),
        () -> assertEquals(1, entity.getCompetencyLevels().get(0).getLevelNumber()),
        () -> assertEquals("Basic", entity.getCompetencyLevels().get(0).getLevelName()),
        () -> assertEquals("Beginner", entity.getCompetencyLevels().get(0).getLevelDescription()),
        () -> assertEquals(2, entity.getCompetencyLevels().get(1).getLevelNumber()),
        () -> assertEquals("Advanced", entity.getCompetencyLevels().get(1).getLevelName()),
        // The nested mapper does not wire the back-reference; MasterEntityServiceImpl
        // sets masterEntity on each level after mapping.
        () -> assertNull(entity.getCompetencyLevels().get(0).getMasterEntity(),
            "back-reference is left null by the mapper")
    );

    assertAll(
        () -> assertNull(entity.getId(), "unmapped — assigned by the database"),
        () -> assertNull(entity.getCreatedAt(), "unmapped — MasterEntityServiceImpl stamps it"),
        () -> assertNull(entity.getCreatedBy(), "unmapped — MasterEntityServiceImpl stamps it"),
        () -> assertNull(entity.getUpdatedAt()),
        () -> assertNull(entity.getUpdatedBy()),
        () -> assertNull(entity.getReviewedAt()),
        () -> assertNull(entity.getReviewedBy())
    );
  }

  /**
   * With no competencyLevels on the request the mapped entity's collection is null, not
   * empty — the field initializer on MasterEntity is discarded by @Builder. Any code that
   * calls getCompetencyLevels().stream() on a freshly mapped entity dereferences null.
   */
  @Test
  @DisplayName("toEntity(EntityCreateRequestDTO) - competencyLevels is null, not empty, when absent")
  void shouldLeaveCompetencyLevelsNullWhenRequestHasNone() {
    EntityCreateRequestDTO request = new EntityCreateRequestDTO();
    request.setCode("R001");
    request.setEntityType("ROLE");

    MasterEntity entity = mapper.toEntity(request);

    assertAll(
        () -> assertNotNull(entity),
        () -> assertNull(entity.getCompetencyLevels()),
        () -> assertNull(entity.getAdditionalProperties())
    );
  }

  // ─── toResponseDTO(MasterEntity) — the update-API response ────────────────────

  @Test
  @DisplayName("toResponseDTO(MasterEntity) - should map every field and nested competency levels")
  void shouldMapEntityToResponseDTO() {
    MasterEntity entity = MasterEntity.builder()
        .id(42)
        .entityId("E-1")
        .entityType("COMPETENCY")
        .type("Behavioural")
        .area("Clinical")
        .code("C001")
        .name("Communication")
        .description("Communicates clearly")
        .status("Active")
        .languageCode("en")
        .level("L1")
        .levelId("L-9")
        .source("manual")
        .additionalProperties(Map.of("region", "north"))
        .createdAt(CREATED_AT)
        .updatedAt(UPDATED_AT)
        .createdBy("creator")
        .updatedBy("editor")
        .competencyLevels(List.of(
            CompetencyLevel.builder().levelNumber(1).levelName("Basic").levelDescription("Beginner").build()))
        .build();

    EntityResponseDTO dto = mapper.toResponseDTO(entity);

    assertAll(
        () -> assertEquals(42, dto.getId()),
        () -> assertEquals("E-1", dto.getEntityId()),
        () -> assertEquals("COMPETENCY", dto.getEntityType()),
        () -> assertEquals("Behavioural", dto.getType()),
        () -> assertEquals("Clinical", dto.getArea()),
        () -> assertEquals("C001", dto.getCode()),
        () -> assertEquals("Communication", dto.getName()),
        () -> assertEquals("Communicates clearly", dto.getDescription()),
        () -> assertEquals("Active", dto.getStatus()),
        () -> assertEquals("en", dto.getLanguageCode()),
        () -> assertEquals("L1", dto.getLevel()),
        () -> assertEquals("L-9", dto.getLevelId()),
        () -> assertEquals("manual", dto.getSource()),
        () -> assertEquals(Map.of("region", "north"), dto.getAdditionalProperties()),
        () -> assertEquals(CREATED_AT, dto.getCreatedAt()),
        () -> assertEquals(UPDATED_AT, dto.getUpdatedAt()),
        () -> assertEquals("creator", dto.getCreatedBy()),
        () -> assertEquals("editor", dto.getUpdatedBy())
    );

    List<CompetencyLevelResponseDTO> levels = dto.getCompetencyLevels();
    assertAll(
        () -> assertEquals(1, levels.size()),
        () -> assertEquals(1, levels.get(0).getLevelNumber()),
        () -> assertEquals("Basic", levels.get(0).getLevelName()),
        () -> assertEquals("Beginner", levels.get(0).getLevelDescription())
    );
  }

  @Test
  @DisplayName("toResponseDTO(MasterEntity) - should leave competencyLevels null when the entity has none")
  void shouldLeaveResponseCompetencyLevelsNull() {
    MasterEntity entity = MasterEntity.builder().code("R001").competencyLevels(null).build();

    assertNull(mapper.toResponseDTO(entity).getCompetencyLevels());
  }

  @Test
  @DisplayName("toResponseDTO(CompetencyLevel) - should map the three level fields")
  void shouldMapCompetencyLevelToResponseDTO() {
    CompetencyLevelResponseDTO dto = mapper.toResponseDTO(CompetencyLevel.builder()
        .id(7).levelNumber(3).levelName("Proficient").levelDescription("Independent").build());

    assertAll(
        () -> assertEquals(3, dto.getLevelNumber()),
        () -> assertEquals("Proficient", dto.getLevelName()),
        () -> assertEquals("Independent", dto.getLevelDescription())
    );
  }

  @Test
  @DisplayName("toCompetencyLevel - should map level fields and leave id and back-reference unset")
  void shouldMapDTOToCompetencyLevel() {
    CompetencyLevel level = mapper.toCompetencyLevel(CompetencyLevelDTO.builder()
        .levelNumber(2).levelName("Intermediate").levelDescription("Guided").build());

    assertAll(
        () -> assertEquals(2, level.getLevelNumber()),
        () -> assertEquals("Intermediate", level.getLevelName()),
        () -> assertEquals("Guided", level.getLevelDescription()),
        () -> assertNull(level.getId(), "unmapped — assigned by the database"),
        () -> assertNull(level.getMasterEntity(),
            "unmapped — MasterEntityServiceImpl sets the parent back-reference")
    );
  }

  // ─── toSearchResponse — the search-API response ───────────────────────────────

  @Test
  @DisplayName("toSearchResponseDTO - should map document fields and leave levels unset")
  void shouldMapDocumentToSearchResponseDTO() {
    MasterEntitySearchResponseDTO dto = mapper.toSearchResponseDTO(fullDocument());

    assertAll(
        () -> assertEquals("C001_en", dto.getId(), "the document id is a String and is copied as-is"),
        () -> assertEquals("E-1", dto.getEntityId()),
        () -> assertEquals("COMPETENCY", dto.getEntityType()),
        () -> assertEquals("Behavioural", dto.getType()),
        () -> assertEquals("C001", dto.getCode()),
        () -> assertEquals("Clinical", dto.getArea()),
        () -> assertEquals("Communication", dto.getName()),
        () -> assertEquals("Communicates clearly", dto.getDescription()),
        () -> assertEquals("Active", dto.getStatus()),
        () -> assertEquals("en", dto.getLanguageCode()),
        () -> assertEquals("L1", dto.getLevel()),
        () -> assertEquals("L-9", dto.getLevelId()),
        () -> assertEquals(Map.of("region", "north"), dto.getAdditionalProperties()),
        () -> assertEquals(CREATED_AT, dto.getCreatedAt()),
        () -> assertEquals(UPDATED_AT, dto.getUpdatedAt()),
        () -> assertEquals("creator", dto.getCreatedBy()),
        () -> assertEquals("editor", dto.getUpdatedBy()),
        () -> assertNull(dto.getLevels(), "levels is @Mapping(ignore) — the default method fills it")
    );
  }

  @Test
  @DisplayName("toSearchResponse - should flatten all five competency level columns into levels")
  void shouldFlattenAllFiveCompetencyLevels() {
    MasterEntitySearchResponseDTO dto = mapper.toSearchResponse(fullDocument());

    List<CompetencyLevelDTO> levels = dto.getLevels();
    assertAll(
        () -> assertEquals(5, levels.size()),
        () -> assertEquals(List.of(1, 2, 3, 4, 5),
            levels.stream().map(CompetencyLevelDTO::getLevelNumber).toList()),
        () -> assertEquals(List.of("Basic", "Intermediate", "Proficient", "Advanced", "Expert"),
            levels.stream().map(CompetencyLevelDTO::getLevelName).toList()),
        () -> assertEquals("Level 3 desc", levels.get(2).getLevelDescription())
    );
  }

  @Test
  @DisplayName("toSearchResponse - should skip gaps and keep the declared level numbers")
  void shouldSkipMissingCompetencyLevels() {
    MasterEntityDocument document = MasterEntityDocument.builder()
        .id("C001_en").code("C001")
        .competencyLevel1Name("Basic").competencyLevel1Description("Beginner")
        .competencyLevel3Name("Proficient").competencyLevel3Description("Independent")
        .build();

    List<CompetencyLevelDTO> levels = mapper.toSearchResponse(document).getLevels();

    assertAll(
        () -> assertEquals(2, levels.size(), "levels 2, 4 and 5 are absent"),
        () -> assertEquals(1, levels.get(0).getLevelNumber()),
        () -> assertEquals(3, levels.get(1).getLevelNumber(),
            "the level number reflects the source column, not the list position")
    );
  }

  @Test
  @DisplayName("toSearchResponse - should return an empty levels list for a non-competency document")
  void shouldReturnEmptyLevelsWhenNoCompetencyColumns() {
    MasterEntityDocument document = MasterEntityDocument.builder()
        .id("R001_en").code("R001").entityType("ROLE").name("Developer").build();

    MasterEntitySearchResponseDTO dto = mapper.toSearchResponse(document);

    assertAll(
        () -> assertNotNull(dto.getLevels()),
        () -> assertTrue(dto.getLevels().isEmpty()),
        () -> assertEquals("Developer", dto.getName())
    );
  }

  @Test
  @DisplayName("mapCompetencyLevels - should skip a level whose name is missing even if it has a description")
  void shouldSkipLevelWithDescriptionButNoName() {
    MasterEntityDocument document = MasterEntityDocument.builder()
        .competencyLevel1Description("orphan description")
        .competencyLevel2Name("Intermediate")
        .build();

    List<CompetencyLevelDTO> levels = mapper.mapCompetencyLevels(document);

    assertAll(
        () -> assertEquals(1, levels.size(), "presence is decided by the name column alone"),
        () -> assertEquals(2, levels.get(0).getLevelNumber())
    );
  }

  // ─── toEntity(MasterEntityDocument) — currently unused by any caller ──────────

  @Test
  @DisplayName("toEntity(MasterEntityDocument) - should map fields, parsing the id as an integer")
  void shouldMapDocumentToEntity() {
    MasterEntityDocument document = fullDocument();
    document.setId("123");

    MasterEntity entity = mapper.toEntity(document);

    assertAll(
        () -> assertEquals(123, entity.getId(), "the String document id is parsed into an Integer"),
        () -> assertEquals("E-1", entity.getEntityId()),
        () -> assertEquals("COMPETENCY", entity.getEntityType()),
        () -> assertEquals("Behavioural", entity.getType()),
        () -> assertEquals("Clinical", entity.getArea()),
        () -> assertEquals("Communication", entity.getName()),
        () -> assertEquals("Communicates clearly", entity.getDescription()),
        () -> assertEquals("Active", entity.getStatus()),
        () -> assertEquals("C001", entity.getCode()),
        () -> assertEquals("en", entity.getLanguageCode()),
        () -> assertEquals(CREATED_AT, entity.getCreatedAt()),
        () -> assertEquals(UPDATED_AT, entity.getUpdatedAt()),
        () -> assertEquals("creator", entity.getCreatedBy()),
        () -> assertEquals("editor", entity.getUpdatedBy()),
        () -> assertEquals(Map.of("region", "north"), entity.getAdditionalProperties())
    );

    assertAll(
        () -> assertNull(entity.getLevel(), "level is @Mapping(ignore)"),
        () -> assertNull(entity.getLevelId(), "levelId is @Mapping(ignore)"),
        () -> assertNull(entity.getSource(), "source is @Mapping(ignore)"),
        () -> assertNull(entity.getReviewedAt(), "reviewedAt is @Mapping(ignore)"),
        () -> assertNull(entity.getReviewedBy(), "reviewedBy is @Mapping(ignore)"),
        () -> assertNull(entity.getCompetencyLevels(), "competencyLevels is unmapped")
    );
  }

  @Test
  @DisplayName("toEntity(MasterEntityDocument) - should leave id null when the document has none")
  void shouldLeaveEntityIdNullWhenDocumentIdIsNull() {
    MasterEntityDocument document = MasterEntityDocument.builder().code("C001").build();

    assertNull(mapper.toEntity(document).getId());
  }

  /**
   * Documents a landmine rather than desired behaviour. Every document this service writes
   * gets id = code + "_" + languageCode (MasterEntityEsServiceImpl.saveEntityDetailsInES),
   * and Integer.parseInt cannot read that. The mapping survives only because no caller in
   * src/main invokes toEntity(MasterEntityDocument) today.
   */
  @Test
  @DisplayName("toEntity(MasterEntityDocument) - should throw on the code_language id format this service writes")
  void shouldThrowWhenDocumentIdIsNotNumeric() {
    MasterEntityDocument document = MasterEntityDocument.builder().id("C001_en").code("C001").build();

    assertThrows(NumberFormatException.class, () -> mapper.toEntity(document));
  }

  // ─── Fixtures ────────────────────────────────────────────────────────────────

  private static EntitySheetRow fullSheetRow() {
    return EntitySheetRow.builder()
        .rowNumber("2")
        .code("C001")
        .language("en")
        .createdDate(CREATED_AT)
        .entityId("E-1")
        .entityType("COMPETENCY")
        .type("Behavioural")
        .area("Clinical")
        .name("Communication")
        .description("Communicates clearly")
        .levelId("L-9")
        .createdBy("uploader")
        .updatedBy("editor")
        .reviewedBy("reviewer")
        .competencyLevel1Name("Basic").competencyLevel1Description("Level 1 desc")
        .competencyLevel2Name("Intermediate").competencyLevel2Description("Level 2 desc")
        .competencyLevel3Name("Proficient").competencyLevel3Description("Level 3 desc")
        .competencyLevel4Name("Advanced").competencyLevel4Description("Level 4 desc")
        .competencyLevel5Name("Expert").competencyLevel5Description("Level 5 desc")
        .build();
  }

  private static MasterEntityDocument fullDocument() {
    return MasterEntityDocument.builder()
        .id("C001_en")
        .entityId("E-1")
        .entityType("COMPETENCY")
        .type("Behavioural")
        .code("C001")
        .area("Clinical")
        .name("Communication")
        .description("Communicates clearly")
        .status("Active")
        .additionalProperties(Map.of("region", "north"))
        .level("L1")
        .levelId("L-9")
        .languageCode("en")
        .createdAt(CREATED_AT)
        .updatedAt(UPDATED_AT)
        .createdBy("creator")
        .updatedBy("editor")
        .competencyLevel1Name("Basic").competencyLevel1Description("Level 1 desc")
        .competencyLevel2Name("Intermediate").competencyLevel2Description("Level 2 desc")
        .competencyLevel3Name("Proficient").competencyLevel3Description("Level 3 desc")
        .competencyLevel4Name("Advanced").competencyLevel4Description("Level 4 desc")
        .competencyLevel5Name("Expert").competencyLevel5Description("Level 5 desc")
        .build();
  }
}
