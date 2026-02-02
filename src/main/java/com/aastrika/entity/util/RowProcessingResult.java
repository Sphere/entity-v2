package com.aastrika.entity.util;

import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
public class RowProcessingResult {

    private int totalRows;
    private int successCount;
    private int failedCount;

    @Builder.Default
    private List<RowStatus> rowStatuses = new ArrayList<>();

    public void addSuccess(int rowNumber) {
        rowStatuses.add(RowStatus.success(rowNumber));
        successCount++;
    }

    public void addFailure(int rowNumber, List<String> missingFields, String errorMessage) {
        rowStatuses.add(RowStatus.failure(rowNumber, missingFields, errorMessage));
        failedCount++;
    }

    public boolean hasFailures() {
        return failedCount > 0;
    }

    public List<RowStatus> getFailedRows() {
        return rowStatuses.stream()
                .filter(r -> !r.isSuccess())
                .toList();
    }

    @Data
    @Builder
    public static class RowStatus {
        private int rowNumber;
        private boolean success;
        private String errorMessage;

        @Builder.Default
        private List<String> missingFields = new ArrayList<>();

        public static RowStatus success(int rowNumber) {
            return RowStatus.builder()
                    .rowNumber(rowNumber)
                    .success(true)
                    .build();
        }

        public static RowStatus failure(int rowNumber, List<String> missingFields, String errorMessage) {
            return RowStatus.builder()
                    .rowNumber(rowNumber)
                    .success(false)
                    .missingFields(missingFields != null ? missingFields : new ArrayList<>())
                    .errorMessage(errorMessage)
                    .build();
        }
    }
}
