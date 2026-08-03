package com.aastrika.entity.reader;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.aastrika.entity.dto.EntitySheetRow;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFFormulaEvaluator;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

/**
 * Drives XlsxEntitySheetReader against real workbooks built in memory with POI.
 *
 * <p>Note that {@code read} is not part of the EntitySheetReader interface — the
 * {@code @Override} on it is commented out — and the interface method the upload flow
 * actually calls, {@code getCompiledEntitySheet}, is a stub. Both are covered here.
 */
class XlsxEntitySheetReaderTest {

  private final XlsxEntitySheetReader reader = new XlsxEntitySheetReader();

  // ─── The interface methods the upload flow uses ───────────────────────────────

  /**
   * Documents current behaviour, not desired behaviour. The factory advertises XLSX support
   * and {@code supports} returns true for .xlsx, but getCompiledEntitySheet returns an empty
   * map — so MasterEntityServiceImpl.saveSheetDataIntoDB finds no entry for the global entity
   * type, imports nothing, and still returns a success response with a null upload tracker.
   */
  @Test
  @DisplayName("getCompiledEntitySheet - returns an empty map, so an XLSX upload imports nothing")
  void shouldReturnEmptyMapFromCompiledEntitySheet() {
    MultipartFile file = workbook(sheet -> {
      writeRow(sheet, 0, "code", "name", "entity_type");
      writeRow(sheet, 1, "R001", "Developer", "ROLE");
    });

    assertTrue(reader.getCompiledEntitySheet(file).isEmpty(),
        "the XLSX branch of the upload flow is not implemented");
  }

  @Test
  @DisplayName("getGlobalEntityType - returns an empty string rather than a parsed type")
  void shouldReturnEmptyGlobalEntityType() {
    assertEquals("", reader.getGlobalEntityType());
  }

  @Test
  @DisplayName("supports - should accept the xlsx content type or a .xlsx filename")
  void shouldSupportOnlyXlsx() {
    assertAll(
        () -> assertTrue(reader.supports(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "entities.bin")),
        () -> assertTrue(reader.supports("application/octet-stream", "entities.xlsx")),
        () -> assertTrue(reader.supports(null, "ENTITIES.XLSX"), "the extension check is case-insensitive"),
        () -> assertFalse(reader.supports(null, null)),
        () -> assertFalse(reader.supports("text/csv", "entities.csv"))
    );
  }

  // ─── read ────────────────────────────────────────────────────────────────────

  @Test
  @DisplayName("read - should map header columns to fields and stamp createdDate")
  void shouldReadRowsByHeaderName() {
    MultipartFile file = workbook(sheet -> {
      writeRow(sheet, 0, "code", "name", "description", "language", "level_id",
          "created_by", "updated_by", "reviewed_by", "reviewed_date");
      writeRow(sheet, 1, "R001", "Developer", "Writes code", "en", "L-9",
          "uploader", "editor", "reviewer", "2026-07-31");
      writeRow(sheet, 2, "R002", "Tester", "Tests code", "en", "L-8",
          "uploader", "editor", "reviewer", "2026-07-30");
    });

    List<EntitySheetRow> rows = reader.read(file);

    assertEquals(2, rows.size());
    EntitySheetRow first = rows.get(0);
    assertAll(
        () -> assertEquals("R001", first.getCode()),
        () -> assertEquals("Developer", first.getName()),
        () -> assertEquals("Writes code", first.getDescription()),
        () -> assertEquals("en", first.getLanguage()),
        () -> assertEquals("L-9", first.getLevelId()),
        () -> assertEquals("uploader", first.getCreatedBy()),
        () -> assertEquals("editor", first.getUpdatedBy()),
        () -> assertEquals("reviewer", first.getReviewedBy()),
        () -> assertEquals("2026-07-31", first.getReviewedDate()),
        () -> assertNotNull(first.getCreatedDate(), "createdDate is stamped, not read from a column"),
        () -> assertEquals("R002", rows.get(1).getCode())
    );
  }

  /**
   * mapRowToEntityRow reads neither entity_type nor id, so even the working read path cannot
   * determine which entity type an XLSX sheet holds.
   */
  @Test
  @DisplayName("read - should ignore the entity_type and id columns entirely")
  void shouldNotMapEntityTypeOrIdColumns() {
    MultipartFile file = workbook(sheet -> {
      writeRow(sheet, 0, "code", "name", "entity_type", "id");
      writeRow(sheet, 1, "R001", "Developer", "ROLE", "E1");
    });

    EntitySheetRow row = reader.read(file).get(0);

    assertAll(
        () -> assertEquals("R001", row.getCode()),
        () -> assertNull(row.getEntityType(), "no entity_type mapping exists in the XLSX reader"),
        () -> assertNull(row.getEntityId(), "no id mapping exists in the XLSX reader")
    );
  }

  @Test
  @DisplayName("read - should map all ten competency level columns")
  void shouldReadCompetencyLevelColumns() {
    MultipartFile file = workbook(sheet -> {
      writeRow(sheet, 0, "code",
          "competency_level_1_name", "competency_level_1_description",
          "competency_level_2_name", "competency_level_2_description",
          "competency_level_3_name", "competency_level_3_description",
          "competency_level_4_name", "competency_level_4_description",
          "competency_level_5_name", "competency_level_5_description");
      writeRow(sheet, 1, "C001",
          "Basic", "L1 desc", "Intermediate", "L2 desc", "Proficient", "L3 desc",
          "Advanced", "L4 desc", "Expert", "L5 desc");
    });

    EntitySheetRow row = reader.read(file).get(0);

    assertAll(
        () -> assertEquals("Basic", row.getCompetencyLevel1Name()),
        () -> assertEquals("L1 desc", row.getCompetencyLevel1Description()),
        () -> assertEquals("Intermediate", row.getCompetencyLevel2Name()),
        () -> assertEquals("L2 desc", row.getCompetencyLevel2Description()),
        () -> assertEquals("Proficient", row.getCompetencyLevel3Name()),
        () -> assertEquals("L3 desc", row.getCompetencyLevel3Description()),
        () -> assertEquals("Advanced", row.getCompetencyLevel4Name()),
        () -> assertEquals("L4 desc", row.getCompetencyLevel4Description()),
        () -> assertEquals("Expert", row.getCompetencyLevel5Name()),
        () -> assertEquals("L5 desc", row.getCompetencyLevel5Description())
    );
  }

  @Test
  @DisplayName("read - should convert numeric, boolean, formula and blank cells to strings")
  void shouldConvertEveryCellTypeToString() {
    MultipartFile file = workbook(sheet -> {
      writeRow(sheet, 0, "name", "level_id", "type", "description", "created_by", "code");

      Row row = sheet.createRow(1);
      row.createCell(0).setCellValue("Developer");                  // STRING
      row.createCell(1).setCellValue(42);                           // NUMERIC
      row.createCell(2).setCellValue(true);                         // BOOLEAN
      row.createCell(3).setCellFormula("CONCATENATE(\"a\",\"b\")"); // FORMULA → String
      row.createCell(4).setCellFormula("1+2");                      // FORMULA → numeric
      row.createCell(5, CellType.BLANK);                            // BLANK

      XSSFFormulaEvaluator.evaluateAllFormulaCells((XSSFWorkbook) sheet.getWorkbook());
    });

    EntitySheetRow row = reader.read(file).get(0);

    assertAll(
        () -> assertEquals("Developer", row.getName()),
        () -> assertEquals("42", row.getLevelId(), "a whole number loses its .0"),
        () -> assertEquals("true", row.getType()),
        () -> assertEquals("ab", row.getDescription(), "a string formula yields its cached text"),
        () -> assertEquals("3.0", row.getCreatedBy(), "a numeric formula keeps its decimal form"),
        () -> assertNull(row.getCode(), "a blank cell maps to null")
    );
  }

  @Test
  @DisplayName("read - should render a date-formatted numeric cell as a local date-time")
  void shouldRenderDateFormattedCells() {
    MultipartFile file = workbook(sheet -> {
      writeRow(sheet, 0, "name", "reviewed_date");

      Workbook wb = sheet.getWorkbook();
      CellStyle dateStyle = wb.createCellStyle();
      dateStyle.setDataFormat(wb.getCreationHelper().createDataFormat().getFormat("yyyy-mm-dd"));

      Row row = sheet.createRow(1);
      row.createCell(0).setCellValue("Developer");
      Cell dateCell = row.createCell(1);
      // Set from LocalDateTime rather than Date so the assertion does not depend on the
      // JVM's default timezone.
      dateCell.setCellValue(LocalDateTime.of(2026, 7, 31, 9, 30));
      dateCell.setCellStyle(dateStyle);
    });

    EntitySheetRow row = reader.read(file).get(0);

    assertEquals("2026-07-31T09:30", row.getReviewedDate(),
        "a date-formatted numeric cell is rendered via getLocalDateTimeCellValue");
  }

  @Test
  @DisplayName("read - should skip entirely blank rows")
  void shouldSkipBlankRows() {
    MultipartFile file = workbook(sheet -> {
      writeRow(sheet, 0, "code", "name");
      writeRow(sheet, 1, "R001", "Developer");
      Row blank = sheet.createRow(2);
      blank.createCell(0).setCellValue("");
      blank.createCell(1).setCellValue("   ");
      writeRow(sheet, 3, "R002", "Tester");
    });

    List<EntitySheetRow> rows = reader.read(file);

    assertAll(
        () -> assertEquals(2, rows.size(), "the whitespace-only row is dropped"),
        () -> assertEquals("R001", rows.get(0).getCode()),
        () -> assertEquals("R002", rows.get(1).getCode())
    );
  }

  @Test
  @DisplayName("read - should ignore blank header cells when building the header map")
  void shouldIgnoreBlankHeaderCells() {
    MultipartFile file = workbook(sheet -> {
      writeRow(sheet, 0, "code", "   ", "name");
      writeRow(sheet, 1, "R001", "orphan value", "Developer");
    });

    EntitySheetRow row = reader.read(file).get(0);

    assertAll(
        () -> assertEquals("R001", row.getCode()),
        () -> assertEquals("Developer", row.getName())
    );
  }

  @Test
  @DisplayName("read - should trim header names before matching")
  void shouldTrimHeaderNames() {
    MultipartFile file = workbook(sheet -> {
      writeRow(sheet, 0, "  code  ", " name ");
      writeRow(sheet, 1, "R001", "Developer");
    });

    EntitySheetRow row = reader.read(file).get(0);

    assertAll(
        () -> assertEquals("R001", row.getCode()),
        () -> assertEquals("Developer", row.getName())
    );
  }

  @Test
  @DisplayName("read - should return an empty list for a workbook with no rows")
  void shouldReturnEmptyListForEmptySheet() {
    assertTrue(reader.read(workbook(sheet -> { })).isEmpty());
  }

  @Test
  @DisplayName("read - should leave every field null when no header row matches")
  void shouldReturnNullFieldsWhenHeadersAreUnknown() {
    MultipartFile file = workbook(sheet -> {
      writeRow(sheet, 0, "unknown_column");
      writeRow(sheet, 1, "some value");
    });

    EntitySheetRow row = reader.read(file).get(0);

    assertAll(
        () -> assertEquals(1, reader.read(file).size()),
        () -> assertNull(row.getCode()),
        () -> assertNull(row.getName()),
        () -> assertNotNull(row.getCreatedDate(), "createdDate is stamped regardless of headers")
    );
  }

  @Test
  @DisplayName("read - should wrap an unreadable file in a RuntimeException")
  void shouldWrapFailureReadingCorruptFile() {
    MultipartFile file = new MockMultipartFile("file", "entities.xlsx",
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        "not a workbook".getBytes(StandardCharsets.UTF_8));

    RuntimeException ex = assertThrows(RuntimeException.class, () -> reader.read(file));

    assertAll(
        () -> assertEquals("Error reading XLSX file", ex.getMessage()),
        () -> assertNotNull(ex.getCause())
    );
  }

  // ─── Helpers ─────────────────────────────────────────────────────────────────

  @FunctionalInterface
  private interface SheetBuilder {
    void build(Sheet sheet);
  }

  private static MultipartFile workbook(SheetBuilder builder) {
    try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      builder.build(wb.createSheet("entities"));
      wb.write(out);
      return new MockMultipartFile("file", "entities.xlsx",
          "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", out.toByteArray());
    } catch (Exception e) {
      throw new IllegalStateException("Failed to build the test workbook", e);
    }
  }

  private static void writeRow(Sheet sheet, int rowIndex, String... values) {
    Row row = sheet.createRow(rowIndex);
    for (int i = 0; i < values.length; i++) {
      row.createCell(i).setCellValue(values[i]);
    }
  }
}
