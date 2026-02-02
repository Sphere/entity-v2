package com.aastrika.entity.reader;

import com.aastrika.entity.common.EntitySheetHeaders;
import com.aastrika.entity.dto.EntitySheetRow;
import java.util.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
@Slf4j
public class XlsxEntitySheetReader implements EntitySheetReader {

  @Override
  public List<EntitySheetRow> read(MultipartFile file) {
    log.info("Reading XLSX file: {}", file.getOriginalFilename());
    List<EntitySheetRow> entitySheetRow = new ArrayList<>();

    try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
      Sheet sheet = workbook.getSheetAt(0);
      Map<String, Integer> headerMap = buildHeaderMap(sheet.getRow(0));

      for (int i = 1; i <= sheet.getLastRowNum(); i++) {
        Row row = sheet.getRow(i);
        if (row != null && !isRowEmpty(row)) {
          EntitySheetRow entityRow = mapRowToEntityRow(row, headerMap);
          entitySheetRow.add(entityRow);
        }
      }
    } catch (Exception e) {
      throw new RuntimeException("Error reading XLSX file", e);
    }

    log.info("Read {} rows from XLSX", entitySheetRow.size());
    return entitySheetRow;
  }

  @Override
  public boolean supports(String contentType, String fileName) {
    return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet".equals(contentType)
        || (fileName != null && fileName.toLowerCase().endsWith(".xlsx"));
  }

  private Map<String, Integer> buildHeaderMap(Row headerRow) {
    Map<String, Integer> headerMap = new HashMap<>();
    if (headerRow != null) {
      for (Cell cell : headerRow) {
        String headerName = getCellValueAsString(cell);
        if (headerName != null && !headerName.isBlank()) {
          headerMap.put(headerName.trim(), cell.getColumnIndex());
        }
      }
    }
    return headerMap;
  }

  private EntitySheetRow mapRowToEntityRow(Row row, Map<String, Integer> headerMap) {
    return EntitySheetRow.builder()
        .type(getCellValue(row, headerMap, EntitySheetHeaders.TYPE))
        .name(getCellValue(row, headerMap, EntitySheetHeaders.NAME))
        .description(getCellValue(row, headerMap, EntitySheetHeaders.DESCRIPTION))
        .language(getCellValue(row, headerMap, EntitySheetHeaders.LANGUAGE))
        .code(getCellValue(row, headerMap, EntitySheetHeaders.CODE))
        .levelId(getCellValue(row, headerMap, EntitySheetHeaders.LEVEL_ID))
        .createdBy(getCellValue(row, headerMap, EntitySheetHeaders.CREATED_BY))
        .updatedBy(getCellValue(row, headerMap, EntitySheetHeaders.UPDATED_BY))
        .reviewedBy(getCellValue(row, headerMap, EntitySheetHeaders.REVIEWED_BY))
        .createdDate(new Date())
        .updatedDate(getCellValue(row, headerMap, EntitySheetHeaders.UPDATED_DATE))
        .reviewedDate(getCellValue(row, headerMap, EntitySheetHeaders.REVIEWED_DATE))
        //                .additionalProperties(getCellValue(row, headerMap,
        // EntitySheetHeaders.ADDITIONAL_PROPERTIES))
        .competencyLevel1Name(
            getCellValue(row, headerMap, EntitySheetHeaders.COMPETENCY_LEVEL_1_NAME))
        .competencyLevel1Description(
            getCellValue(row, headerMap, EntitySheetHeaders.COMPETENCY_LEVEL_1_DESCRIPTION))
        .competencyLevel2Name(
            getCellValue(row, headerMap, EntitySheetHeaders.COMPETENCY_LEVEL_2_NAME))
        .competencyLevel2Description(
            getCellValue(row, headerMap, EntitySheetHeaders.COMPETENCY_LEVEL_2_DESCRIPTION))
        .competencyLevel3Name(
            getCellValue(row, headerMap, EntitySheetHeaders.COMPETENCY_LEVEL_3_NAME))
        .competencyLevel3Description(
            getCellValue(row, headerMap, EntitySheetHeaders.COMPETENCY_LEVEL_3_DESCRIPTION))
        .competencyLevel4Name(
            getCellValue(row, headerMap, EntitySheetHeaders.COMPETENCY_LEVEL_4_NAME))
        .competencyLevel4Description(
            getCellValue(row, headerMap, EntitySheetHeaders.COMPETENCY_LEVEL_4_DESCRIPTION))
        .competencyLevel5Name(
            getCellValue(row, headerMap, EntitySheetHeaders.COMPETENCY_LEVEL_5_NAME))
        .competencyLevel5Description(
            getCellValue(row, headerMap, EntitySheetHeaders.COMPETENCY_LEVEL_5_DESCRIPTION))
        .build();
  }

  private String getCellValue(Row row, Map<String, Integer> headerMap, String header) {
    Integer colIndex = headerMap.get(header);
    if (colIndex == null) return null;
    Cell cell = row.getCell(colIndex);
    return getCellValueAsString(cell);
  }

  private String getCellValueAsString(Cell cell) {
    if (cell == null) return null;
    return switch (cell.getCellType()) {
      case STRING -> cell.getStringCellValue();
      case NUMERIC -> DateUtil.isCellDateFormatted(cell)
          ? cell.getLocalDateTimeCellValue().toString()
          : String.valueOf((long) cell.getNumericCellValue());
      case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
      case FORMULA -> cell.getCachedFormulaResultType() == CellType.STRING
          ? cell.getStringCellValue()
          : String.valueOf(cell.getNumericCellValue());
      default -> null;
    };
  }

  private boolean isRowEmpty(Row row) {
    for (Cell cell : row) {
      if (cell != null && cell.getCellType() != CellType.BLANK) {
        String value = getCellValueAsString(cell);
        if (value != null && !value.isBlank()) return false;
      }
    }
    return true;
  }
}
