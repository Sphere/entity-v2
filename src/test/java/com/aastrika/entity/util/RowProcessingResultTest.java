package com.aastrika.entity.util;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.aastrika.entity.util.RowProcessingResult.RowStatus;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * RowProcessingResult is referenced by nothing in src/main — no service, reader or controller
 * constructs it. These tests exist to describe its accounting rules, not because the type is
 * in use. If the class is deleted, delete this test with it.
 */
class RowProcessingResultTest {

  @Test
  @DisplayName("builder - should start with no statuses and zeroed counters")
  void shouldStartEmpty() {
    RowProcessingResult result = RowProcessingResult.builder()
        .totalRows(0).successCount(0).failedCount(0).build();

    assertAll(
        () -> assertTrue(result.getRowStatuses().isEmpty()),
        () -> assertEquals(0, result.getSuccessCount()),
        () -> assertEquals(0, result.getFailedCount()),
        () -> assertFalse(result.hasFailures()),
        () -> assertTrue(result.getFailedRows().isEmpty())
    );
  }

  @Test
  @DisplayName("addSuccess - should record a successful row and increment the success count")
  void shouldRecordSuccess() {
    RowProcessingResult result = newResult();

    result.addSuccess(2);
    result.addSuccess(3);

    RowStatus first = result.getRowStatuses().get(0);
    assertAll(
        () -> assertEquals(2, result.getSuccessCount()),
        () -> assertEquals(0, result.getFailedCount()),
        () -> assertFalse(result.hasFailures()),
        () -> assertEquals(2, first.getRowNumber()),
        () -> assertTrue(first.isSuccess()),
        () -> assertNull(first.getErrorMessage()),
        () -> assertTrue(first.getMissingFields().isEmpty())
    );
  }

  @Test
  @DisplayName("addFailure - should record the missing fields and the error message")
  void shouldRecordFailure() {
    RowProcessingResult result = newResult();

    result.addFailure(4, List.of("name", "description"), "Data is missing in sheet");

    RowStatus failure = result.getRowStatuses().get(0);
    assertAll(
        () -> assertEquals(1, result.getFailedCount()),
        () -> assertEquals(0, result.getSuccessCount()),
        () -> assertTrue(result.hasFailures()),
        () -> assertEquals(4, failure.getRowNumber()),
        () -> assertFalse(failure.isSuccess()),
        () -> assertEquals(List.of("name", "description"), failure.getMissingFields()),
        () -> assertEquals("Data is missing in sheet", failure.getErrorMessage())
    );
  }

  @Test
  @DisplayName("addFailure - should substitute an empty list when missing fields are null")
  void shouldDefaultNullMissingFieldsToEmptyList() {
    RowProcessingResult result = newResult();

    result.addFailure(5, null, "Parse error");

    assertAll(
        () -> assertTrue(result.getRowStatuses().get(0).getMissingFields().isEmpty()),
        () -> assertEquals("Parse error", result.getRowStatuses().get(0).getErrorMessage())
    );
  }

  @Test
  @DisplayName("getFailedRows - should return only the failed statuses, in order")
  void shouldReturnOnlyFailedRows() {
    RowProcessingResult result = newResult();

    result.addSuccess(2);
    result.addFailure(3, List.of("code"), "missing code");
    result.addSuccess(4);
    result.addFailure(5, List.of("name"), "missing name");

    List<RowStatus> failed = result.getFailedRows();
    assertAll(
        () -> assertEquals(4, result.getRowStatuses().size()),
        () -> assertEquals(2, failed.size()),
        () -> assertEquals(3, failed.get(0).getRowNumber()),
        () -> assertEquals(5, failed.get(1).getRowNumber()),
        () -> assertEquals(2, result.getSuccessCount()),
        () -> assertEquals(2, result.getFailedCount())
    );
  }

  /** totalRows is a plain field — neither addSuccess nor addFailure maintains it. */
  @Test
  @DisplayName("totalRows - is not maintained by addSuccess or addFailure")
  void shouldNotTrackTotalRowsAutomatically() {
    RowProcessingResult result = newResult();

    result.addSuccess(2);
    result.addFailure(3, List.of("code"), "missing code");

    assertEquals(0, result.getTotalRows(), "the caller is responsible for setting totalRows");

    result.setTotalRows(2);
    assertEquals(2, result.getTotalRows());
  }

  private static RowProcessingResult newResult() {
    return RowProcessingResult.builder().totalRows(0).successCount(0).failedCount(0).build();
  }
}
