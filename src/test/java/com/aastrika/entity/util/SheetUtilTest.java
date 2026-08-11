package com.aastrika.entity.util;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.aastrika.entity.config.EntitySheetProperties;
import com.aastrika.entity.dto.EntitySheetRow;
import com.aastrika.entity.support.TestApplicationProperties;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.NotWritablePropertyException;
import org.springframework.beans.TypeMismatchException;

/**
 * Covers the reflection-based CSV column → EntitySheetRow field assignment.
 *
 * <p>The header→field mappings come from the deployed application.properties, so these tests
 * describe the configuration that actually ships.
 */
class SheetUtilTest {

  private final SheetUtil sheetUtil = new SheetUtil(TestApplicationProperties.entitySheetProperties());

  @Test
  @DisplayName("mapSheetToEntitySheetRow - should assign every configured column to its field")
  void shouldMapConfiguredColumnsToFields() throws IOException {
    List<CSVRecord> records = records(
        "id,entity_type,code,language,name,description,type,area,level_id,"
            + "created_by,updated_by,reviewed_by,reviewed_date",
        "E1,ROLE,R001,en,Developer,Writes code,Technical,Engineering,L-9,"
            + "uploader,editor,reviewer,2026-07-31");

    EntitySheetRow row = sheetUtil.mapSheetToEntitySheetRow(records).get(0);

    assertAll(
        () -> assertEquals("E1", row.getEntityId(), "id → entityId"),
        () -> assertEquals("ROLE", row.getEntityType(), "entity_type → entityType"),
        () -> assertEquals("R001", row.getCode()),
        () -> assertEquals("en", row.getLanguage()),
        () -> assertEquals("Developer", row.getName()),
        () -> assertEquals("Writes code", row.getDescription()),
        () -> assertEquals("Technical", row.getType()),
        () -> assertEquals("Engineering", row.getArea()),
        () -> assertEquals("L-9", row.getLevelId(), "level_id → levelId"),
        () -> assertEquals("uploader", row.getCreatedBy()),
        () -> assertEquals("editor", row.getUpdatedBy()),
        () -> assertEquals("reviewer", row.getReviewedBy()),
        () -> assertEquals("2026-07-31", row.getReviewedDate())
    );
  }

  @Test
  @DisplayName("mapSheetToEntitySheetRow - should map all ten competency level columns")
  void shouldMapCompetencyLevelColumns() throws IOException {
    List<CSVRecord> records = records(
        "code,competency_level_1_name,competency_level_1_description,"
            + "competency_level_5_name,competency_level_5_description",
        "C001,Basic,L1 desc,Expert,L5 desc");

    EntitySheetRow row = sheetUtil.mapSheetToEntitySheetRow(records).get(0);

    assertAll(
        () -> assertEquals("Basic", row.getCompetencyLevel1Name()),
        () -> assertEquals("L1 desc", row.getCompetencyLevel1Description()),
        () -> assertEquals("Expert", row.getCompetencyLevel5Name()),
        () -> assertEquals("L5 desc", row.getCompetencyLevel5Description()),
        () -> assertNull(row.getCompetencyLevel3Name(), "absent columns stay null")
    );
  }

  @Test
  @DisplayName("mapSheetToEntitySheetRow - should number rows from 2, treating the header as row 1")
  void shouldSetRowNumberOffsetByHeader() throws IOException {
    List<CSVRecord> records = records(
        "code,name",
        "R001,First",
        "R002,Second",
        "R003,Third");

    List<EntitySheetRow> rows = sheetUtil.mapSheetToEntitySheetRow(records);

    assertAll(
        () -> assertEquals(3, rows.size()),
        () -> assertEquals("2", rows.get(0).getRowNumber()),
        () -> assertEquals("3", rows.get(1).getRowNumber()),
        () -> assertEquals("4", rows.get(2).getRowNumber(),
            "the long record number is converted to the String rowNumber field")
    );
  }

  @Test
  @DisplayName("mapSheetToEntitySheetRow - should ignore columns that are not in the mapping")
  void shouldIgnoreUnmappedColumns() throws IOException {
    List<CSVRecord> records = records("code,name,unknown_column", "R001,Developer,ignored");

    EntitySheetRow row = sheetUtil.mapSheetToEntitySheetRow(records).get(0);

    assertAll(
        () -> assertEquals("R001", row.getCode()),
        () -> assertEquals("Developer", row.getName())
    );
  }

  @Test
  @DisplayName("mapSheetToEntitySheetRow - should return an empty list for no records")
  void shouldReturnEmptyListForNoRecords() {
    assertTrue(sheetUtil.mapSheetToEntitySheetRow(List.of()).isEmpty());
  }

  @Test
  @DisplayName("mapSheetToEntitySheetRow - should keep blank cells as empty strings")
  void shouldKeepBlankCellsAsEmptyStrings() throws IOException {
    List<CSVRecord> records = records("code,name,description", "R001,,");

    EntitySheetRow row = sheetUtil.mapSheetToEntitySheetRow(records).get(0);

    assertAll(
        () -> assertEquals("", row.getName(), "a blank cell is not converted to null here"),
        () -> assertEquals("", row.getDescription())
    );
  }

  /**
   * The mechanism behind the upload failure asserted end-to-end in
   * CsvEntitySheetReaderTest: two configured columns target fields that are not Strings, and
   * BeanWrapper has no converter from the CSV text, so the assignment throws.
   */
  @Test
  @DisplayName("mapSheetToEntitySheetRow - should fail on columns whose target field is a Date or Map")
  void shouldFailOnNonStringTargetFields() throws IOException {
    List<CSVRecord> dateRecords = records("code,created_date", "R001,2026-07-31");
    List<CSVRecord> mapRecords = records("code,additional_properties", "R001,north");

    assertAll(
        () -> assertThrows(TypeMismatchException.class,
            () -> sheetUtil.mapSheetToEntitySheetRow(dateRecords),
            "created_date → EntitySheetRow.createdDate (java.util.Date)"),
        () -> assertThrows(TypeMismatchException.class,
            () -> sheetUtil.mapSheetToEntitySheetRow(mapRecords),
            "additional_properties → EntitySheetRow.additionalProperties (Map)")
    );
  }

  /**
   * Shows what EntityStartupApplicationRunner's mapping pre-flight check exists to prevent: a
   * header-field-mappings value naming a field that does not exist fails at upload time, once
   * a user is already waiting on the request.
   */
  @Test
  @DisplayName("mapSheetToEntitySheetRow - should fail when a mapping names a field that does not exist")
  void shouldFailWhenMappedFieldDoesNotExist() throws IOException {
    EntitySheetProperties misconfigured = new EntitySheetProperties();
    misconfigured.setHeaderFieldMappings(Map.of("code", "noSuchField"));
    SheetUtil misconfiguredUtil = new SheetUtil(misconfigured);

    List<CSVRecord> records = records("code", "R001");

    assertThrows(NotWritablePropertyException.class,
        () -> misconfiguredUtil.mapSheetToEntitySheetRow(records));
  }

  // ─── Helpers ─────────────────────────────────────────────────────────────────

  private static List<CSVRecord> records(String... lines) throws IOException {
    CSVFormat format = CSVFormat.DEFAULT.builder()
        .setHeader()
        .setSkipHeaderRecord(true)
        .setTrim(true)
        .setIgnoreEmptyLines(true)
        .build();

    try (CSVParser parser = CSVParser.parse(String.join("\n", lines), format)) {
      return parser.getRecords();
    }
  }
}
