package com.aastrika.entity.reader;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aastrika.entity.dto.BatchProcessingResult;
import com.aastrika.entity.dto.EntitySheetRow;
import com.aastrika.entity.exception.HeaderMissingException;
import com.aastrika.entity.exception.SheetDataMissingException;
import com.aastrika.entity.exception.UploadEntityException;
import com.aastrika.entity.mapper.MasterEntityMapper;
import com.aastrika.entity.model.MasterEntity;
import com.aastrika.entity.repository.jpa.MasterEntityRepository;
import com.aastrika.entity.support.EntityTypeExtension;
import com.aastrika.entity.support.TestApplicationProperties;
import com.aastrika.entity.util.SheetUtil;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

/**
 * Drives CsvEntitySheetReader with real CSV bytes through a MockMultipartFile.
 *
 * <p>SheetUtil and EntitySheetProperties are the real collaborators, bound from the deployed
 * application.properties, so these tests exercise the actual header→field mapping rather than
 * a hand-maintained copy of it. Only the JPA repository and the MapStruct mapper are mocked.
 */
@ExtendWith(MockitoExtension.class)
@ExtendWith(EntityTypeExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CsvEntitySheetReaderTest {

  @Mock private MasterEntityMapper masterEntityMapper;
  @Mock private MasterEntityRepository masterEntityRepository;

  private CsvEntitySheetReader reader;

  private static final String REQUIRED_HEADERS = "id,entity_type,name,description,code,language";
  private static final String COMPETENCY_HEADERS =
      "competency_level_1_name,competency_level_1_description,"
          + "competency_level_2_name,competency_level_2_description,"
          + "competency_level_3_name,competency_level_3_description,"
          + "competency_level_4_name,competency_level_4_description,"
          + "competency_level_5_name,competency_level_5_description";
  private static final String COMPETENCY_VALUES =
      "Basic,L1 desc,Intermediate,L2 desc,Proficient,L3 desc,Advanced,L4 desc,Expert,L5 desc";

  @BeforeEach
  void setUp() {
    reader = new CsvEntitySheetReader(masterEntityMapper, masterEntityRepository,
        TestApplicationProperties.entitySheetProperties(),
        new SheetUtil(TestApplicationProperties.entitySheetProperties()));
  }

  // ─── getCompiledEntitySheet — happy paths ────────────────────────────────────

  @Test
  @DisplayName("getCompiledEntitySheet - should key rows by uppercased entity type and map every column")
  void shouldParseNonCompetencySheet() {
    MultipartFile file = csv(
        REQUIRED_HEADERS + ",type,area,level_id,created_by,updated_by,reviewed_by",
        "E1,ROLE,Developer,Writes code,R001,en,Technical,Engineering,L-9,uploader,editor,reviewer");

    Map<String, List<EntitySheetRow>> result = reader.getCompiledEntitySheet(file);

    assertAll(
        () -> assertEquals(Set.of("ROLE"), result.keySet()),
        () -> assertEquals(1, result.get("ROLE").size()),
        () -> assertEquals("ROLE", reader.getGlobalEntityType(),
            "globalEntityType is read back by MasterEntityServiceImpl after parsing")
    );

    EntitySheetRow row = result.get("ROLE").get(0);
    assertAll(
        () -> assertEquals("E1", row.getEntityId(), "the id column maps to entityId"),
        () -> assertEquals("ROLE", row.getEntityType()),
        () -> assertEquals("Developer", row.getName()),
        () -> assertEquals("Writes code", row.getDescription()),
        () -> assertEquals("R001", row.getCode()),
        () -> assertEquals("en", row.getLanguage()),
        () -> assertEquals("Technical", row.getType()),
        () -> assertEquals("Engineering", row.getArea()),
        () -> assertEquals("L-9", row.getLevelId()),
        () -> assertEquals("uploader", row.getCreatedBy()),
        () -> assertEquals("editor", row.getUpdatedBy()),
        () -> assertEquals("reviewer", row.getReviewedBy())
    );
  }

  @Test
  @DisplayName("getCompiledEntitySheet - should number rows from 2, treating the header as row 1")
  void shouldSetRowNumberOffsetByHeader() {
    MultipartFile file = csv(REQUIRED_HEADERS,
        "E1,ROLE,First,First desc,R001,en",
        "E2,ROLE,Second,Second desc,R002,en");

    List<EntitySheetRow> rows = reader.getCompiledEntitySheet(file).get("ROLE");

    assertAll(
        () -> assertEquals(2, rows.size()),
        () -> assertEquals("2", rows.get(0).getRowNumber(), "first data row is sheet row 2"),
        () -> assertEquals("3", rows.get(1).getRowNumber())
    );
  }

  @Test
  @DisplayName("getCompiledEntitySheet - should accept a competency sheet carrying all ten level columns")
  void shouldParseCompetencySheet() {
    MultipartFile file = csv(
        REQUIRED_HEADERS + "," + COMPETENCY_HEADERS,
        "E1,COMPETENCY,Communication,Communicates clearly,C001,en," + COMPETENCY_VALUES);

    Map<String, List<EntitySheetRow>> result = reader.getCompiledEntitySheet(file);
    EntitySheetRow row = result.get("COMPETENCY").get(0);

    assertAll(
        () -> assertEquals(1, result.get("COMPETENCY").size()),
        () -> assertEquals("COMPETENCY", reader.getGlobalEntityType()),
        () -> assertEquals("Basic", row.getCompetencyLevel1Name()),
        () -> assertEquals("L1 desc", row.getCompetencyLevel1Description()),
        () -> assertEquals("Expert", row.getCompetencyLevel5Name()),
        () -> assertEquals("L5 desc", row.getCompetencyLevel5Description())
    );
  }

  @Test
  @DisplayName("getCompiledEntitySheet - should tolerate a blank id since only code, type, name and description are required")
  void shouldAllowBlankOptionalRequiredHeaderValues() {
    MultipartFile file = csv(REQUIRED_HEADERS, ",ROLE,Developer,Writes code,R001,");

    List<EntitySheetRow> rows = reader.getCompiledEntitySheet(file).get("ROLE");

    assertAll(
        () -> assertEquals(1, rows.size()),
        () -> assertEquals("", rows.get(0).getEntityId()),
        () -> assertEquals("", rows.get(0).getLanguage(),
            "a blank language is accepted here — validateMissingData does not check it")
    );
  }

  // ─── getCompiledEntitySheet — validation failures ────────────────────────────

  @Test
  @DisplayName("getCompiledEntitySheet - should throw when the sheet has a header but no data rows")
  void shouldThrowWhenSheetHasNoDataRows() {
    SheetDataMissingException ex = assertThrows(SheetDataMissingException.class,
        () -> reader.getCompiledEntitySheet(csv(REQUIRED_HEADERS)));

    assertAll(
        () -> assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus()),
        () -> assertEquals("There is no data in the sheet", ex.getMessage())
    );
  }

  @Test
  @DisplayName("getCompiledEntitySheet - should report every missing required header")
  void shouldThrowWhenRequiredHeadersAreMissing() {
    MultipartFile file = csv("id,entity_type,name", "E1,ROLE,Developer");

    HeaderMissingException ex = assertThrows(HeaderMissingException.class,
        () -> reader.getCompiledEntitySheet(file));

    assertAll(
        () -> assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus()),
        () -> assertEquals("Missing required attributes", ex.getMessage()),
        () -> assertEquals(Map.of("missingAttribute", List.of("description", "code", "language")),
            ex.getResult(), "the payload names exactly which headers were absent")
    );
  }

  @Test
  @DisplayName("getCompiledEntitySheet - should reject a sheet mixing more than one entity type")
  void shouldThrowWhenSheetMixesEntityTypes() {
    MultipartFile file = csv(REQUIRED_HEADERS,
        "E1,ROLE,Developer,Writes code,R001,en",
        "E2,ACTIVITY,Review,Reviews code,A001,en");

    SheetDataMissingException ex = assertThrows(SheetDataMissingException.class,
        () -> reader.getCompiledEntitySheet(file));

    assertAll(
        () -> assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus()),
        () -> assertTrue(ex.getMessage().contains("more than one entity type"))
    );
  }

  @Test
  @DisplayName("getCompiledEntitySheet - should wrap an unknown entity type as BAD_REQUEST")
  void shouldThrowWhenEntityTypeIsNotConfigured() {
    MultipartFile file = csv(REQUIRED_HEADERS, "E1,BOGUS,Developer,Writes code,R001,en");

    UploadEntityException ex = assertThrows(UploadEntityException.class,
        () -> reader.getCompiledEntitySheet(file));

    assertAll(
        () -> assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus()),
        () -> assertTrue(ex.getMessage().contains("Invalid entityType: 'BOGUS'"),
            "EntityType.validate's IllegalArgumentException is translated, not leaked")
    );
  }

  @Test
  @DisplayName("getCompiledEntitySheet - should reject a blank entity type via EntityType validation")
  void shouldThrowWhenEntityTypeIsBlank() {
    MultipartFile file = csv(REQUIRED_HEADERS, "E1,,Developer,Writes code,R001,en");

    UploadEntityException ex = assertThrows(UploadEntityException.class,
        () -> reader.getCompiledEntitySheet(file));

    assertTrue(ex.getMessage().contains("Invalid entityType: ''"));
  }

  @Test
  @DisplayName("getCompiledEntitySheet - should report the row numbers of every blank required cell")
  void shouldThrowWhenRequiredDataIsMissing() {
    MultipartFile file = csv(REQUIRED_HEADERS,
        "E1,ROLE,,Writes code,R001,en",
        "E2,ROLE,Second,,R002,en");

    SheetDataMissingException ex = assertThrows(SheetDataMissingException.class,
        () -> reader.getCompiledEntitySheet(file));

    @SuppressWarnings("unchecked")
    Map<String, List<Integer>> missing = (Map<String, List<Integer>>) ex.getResult();
    assertAll(
        () -> assertEquals("Data is missing in sheet", ex.getMessage()),
        () -> assertEquals(List.of(2), missing.get("name"), "row 2 is missing its name"),
        () -> assertEquals(List.of(3), missing.get("description"), "row 3 is missing its description")
    );
  }

  @Test
  @DisplayName("getCompiledEntitySheet - should require the competency level headers for a COMPETENCY sheet")
  void shouldThrowWhenCompetencyHeadersAreMissing() {
    MultipartFile file = csv(REQUIRED_HEADERS,
        "E1,COMPETENCY,Communication,Communicates clearly,C001,en");

    HeaderMissingException ex = assertThrows(HeaderMissingException.class,
        () -> reader.getCompiledEntitySheet(file));

    @SuppressWarnings("unchecked")
    Map<String, List<String>> missing = (Map<String, List<String>>) ex.getResult();
    assertAll(
        () -> assertEquals("Missing competency attributes", ex.getMessage()),
        () -> assertEquals(10, missing.get("missingAttribute").size(),
            "all ten level columns are reported"),
        () -> assertTrue(missing.get("missingAttribute").contains("competency_level_1_name"))
    );
  }

  @Test
  @DisplayName("getCompiledEntitySheet - should report blank competency cells for a COMPETENCY sheet")
  void shouldThrowWhenCompetencyDataIsBlank() {
    MultipartFile file = csv(
        REQUIRED_HEADERS + "," + COMPETENCY_HEADERS,
        "E1,COMPETENCY,Communication,Communicates clearly,C001,en,"
            + "Basic,L1 desc,Intermediate,L2 desc,Proficient,L3 desc,Advanced,L4 desc,,");

    SheetDataMissingException ex = assertThrows(SheetDataMissingException.class,
        () -> reader.getCompiledEntitySheet(file));

    @SuppressWarnings("unchecked")
    Map<String, List<Integer>> missing = (Map<String, List<Integer>>) ex.getResult();
    assertAll(
        () -> assertEquals(List.of(2), missing.get("competency_level_5_name")),
        () -> assertEquals(List.of(2), missing.get("competency_level_5_description")),
        () -> assertNull(missing.get("competency_level_1_name"), "populated levels are not reported")
    );
  }

  @Test
  @DisplayName("getCompiledEntitySheet - should translate an unreadable stream into INTERNAL_SERVER_ERROR")
  void shouldTranslateIoExceptionToInternalServerError() throws IOException {
    MultipartFile file = mock(MultipartFile.class);
    when(file.getOriginalFilename()).thenReturn("entities.csv");
    when(file.getInputStream()).thenThrow(new IOException("stream closed"));

    UploadEntityException ex = assertThrows(UploadEntityException.class,
        () -> reader.getCompiledEntitySheet(file));

    assertAll(
        () -> assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, ex.getStatus()),
        () -> assertEquals("Error reading CSV file", ex.getMessage())
    );
  }

  /**
   * Documents a live gap rather than desired behaviour. Both {@code created_date} and
   * {@code additional_properties} are configured in entity-sheet.header-field-mappings, but
   * the EntitySheetRow fields behind them are Date and Map — BeanWrapper has no converter
   * from the CSV String, so any upload carrying either column fails with a raw Spring
   * TypeMismatchException that escapes the reader's catch blocks untranslated.
   *
   * <p>EntityStartupApplicationRunner only checks that the target field exists, not that the
   * CSV String can be converted into it, so this passes the startup pre-flight check.
   */
  @Test
  @DisplayName("getCompiledEntitySheet - configured date and map columns cannot be parsed from CSV")
  void shouldFailOnColumnsWhoseTargetFieldIsNotAString() {
    MultipartFile dateFile = csv(REQUIRED_HEADERS + ",created_date",
        "E1,ROLE,Developer,Writes code,R001,en,2026-07-31");
    MultipartFile mapFile = csv(REQUIRED_HEADERS + ",additional_properties",
        "E1,ROLE,Developer,Writes code,R001,en,north");

    assertAll(
        () -> assertInstanceOf(TypeMismatchException.class,
            assertThrows(RuntimeException.class, () -> reader.getCompiledEntitySheet(dateFile)),
            "created_date → EntitySheetRow.createdDate (java.util.Date)"),
        () -> assertInstanceOf(TypeMismatchException.class,
            assertThrows(RuntimeException.class, () -> reader.getCompiledEntitySheet(mapFile)),
            "additional_properties → EntitySheetRow.additionalProperties (Map)")
    );
  }

  // ─── supports ────────────────────────────────────────────────────────────────

  @Test
  @DisplayName("supports - should accept text/csv or a .csv filename and reject anything else")
  void shouldSupportOnlyCsv() {
    assertAll(
        () -> assertTrue(reader.supports("text/csv", "entities.txt")),
        () -> assertTrue(reader.supports("application/octet-stream", "entities.csv")),
        () -> assertTrue(reader.supports(null, "ENTITIES.CSV"), "the extension check is case-insensitive"),
        () -> assertFalse(reader.supports(null, null)),
        () -> assertFalse(reader.supports("application/vnd.ms-excel", "entities.xlsx"))
    );
  }

  // ─── readAndSaveInBatches ────────────────────────────────────────────────────

  @Test
  @DisplayName("readAndSaveInBatches - should save full batches plus the remainder and count successes")
  void shouldSaveRecordsInBatches() {
    MultipartFile file = csv(REQUIRED_HEADERS,
        "E1,ROLE,One,Desc one,R001,en",
        "E2,ROLE,Two,Desc two,R002,en",
        "E3,ROLE,Three,Desc three,R003,en",
        "E4,ROLE,Four,Desc four,R004,en",
        "E5,ROLE,Five,Desc five,R005,en");

    when(masterEntityMapper.toEntity(any(EntitySheetRow.class)))
        .thenReturn(MasterEntity.builder().code("R001").build());

    BatchProcessingResult result = reader.readAndSaveInBatches(file, 2);

    assertAll(
        () -> assertEquals(5, result.getTotalRecords()),
        () -> assertEquals(5, result.getSuccessCount()),
        () -> assertEquals(0, result.getFailedCount()),
        () -> assertFalse(result.hasErrors()),
        () -> assertEquals(3, result.getLastSuccessfulBatch(), "batches of 2, 2 and a final 1")
    );
    verify(masterEntityRepository, times(3)).saveAll(anyList());
  }

  @Test
  @DisplayName("readAndSaveInBatches - should record one error per row when a batch fails to save")
  void shouldRecordErrorsWhenBatchSaveFails() {
    MultipartFile file = csv(REQUIRED_HEADERS,
        "E1,ROLE,One,Desc one,R001,en",
        "E2,ROLE,Two,Desc two,R002,en");

    when(masterEntityMapper.toEntity(any(EntitySheetRow.class)))
        .thenReturn(MasterEntity.builder().code("R001").build());
    when(masterEntityRepository.saveAll(anyList()))
        .thenThrow(new RuntimeException("unique constraint violated"));

    BatchProcessingResult result = reader.readAndSaveInBatches(file, 2);

    assertAll(
        () -> assertEquals(2, result.getTotalRecords()),
        () -> assertEquals(0, result.getSuccessCount()),
        () -> assertEquals(2, result.getFailedCount(), "every row in the failed batch is reported"),
        () -> assertTrue(result.hasErrors()),
        () -> assertTrue(result.getErrors().get(0).getErrorMessage()
            .contains("DB save error: unique constraint violated"))
    );
  }

  @Test
  @DisplayName("readAndSaveInBatches - should report zero records for a header-only file")
  void shouldReportNoRecordsForHeaderOnlyFile() {
    BatchProcessingResult result = reader.readAndSaveInBatches(csv(REQUIRED_HEADERS), 10);

    assertAll(
        () -> assertEquals(0, result.getTotalRecords()),
        () -> assertEquals(0, result.getSuccessCount()),
        () -> assertFalse(result.hasErrors())
    );
    verify(masterEntityRepository, times(0)).saveAll(anyList());
  }

  @Test
  @DisplayName("readAndSaveInBatches - should wrap an unreadable stream in a RuntimeException")
  void shouldWrapIoFailureDuringBatchProcessing() throws IOException {
    MultipartFile file = mock(MultipartFile.class);
    when(file.getInputStream()).thenThrow(new IOException("stream closed"));

    RuntimeException ex = assertThrows(RuntimeException.class,
        () -> reader.readAndSaveInBatches(file, 10));

    assertAll(
        () -> assertEquals("Error reading CSV file", ex.getMessage()),
        () -> assertNotNull(ex.getCause())
    );
  }

  @Test
  @DisplayName("readAndSaveInBatches - should map only the columns present, leaving absent ones null")
  void shouldLeaveUnmappedColumnsNullDuringBatchProcessing() {
    MultipartFile file = csv(REQUIRED_HEADERS, "E1,ROLE,One,Desc one,R001,en");

    when(masterEntityMapper.toEntity(any(EntitySheetRow.class)))
        .thenAnswer(invocation -> {
          EntitySheetRow row = invocation.getArgument(0);
          assertAll(
              () -> assertEquals("R001", row.getCode()),
              () -> assertEquals("One", row.getName()),
              () -> assertNull(row.getLevelId(), "level_id column absent → null, not empty string"),
              () -> assertNull(row.getCompetencyLevel1Name())
          );
          return MasterEntity.builder().code("R001").build();
        });

    BatchProcessingResult result = reader.readAndSaveInBatches(file, 10);

    assertEquals(1, result.getSuccessCount());
  }

  // ─── Helpers ─────────────────────────────────────────────────────────────────

  private static MultipartFile csv(String... lines) {
    return new MockMultipartFile("file", "entities.csv", "text/csv",
        String.join("\n", lines).getBytes(StandardCharsets.UTF_8));
  }
}
