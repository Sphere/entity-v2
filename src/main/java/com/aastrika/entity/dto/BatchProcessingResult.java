package com.aastrika.entity.dto;

import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BatchProcessingResult {

  private int totalRecords;
  private int successCount;
  private int failedCount;
  private int lastSuccessfulBatch;
  private int lastSuccessfulRowIndex;

  @Builder.Default private List<BatchError> errors = new ArrayList<>();

  public void addError(int batchNumber, int rowIndex, String errorMessage) {
    errors.add(new BatchError(batchNumber, rowIndex, errorMessage));
    failedCount++;
  }

  public void incrementSuccess(int count) {
    successCount += count;
  }

  public boolean hasErrors() {
    return !errors.isEmpty();
  }

  @Data
  public static class BatchError {
    private final int batchNumber;
    private final int rowIndex;
    private final String errorMessage;
  }
}
