package com.aastrika.entity.reader;

import com.aastrika.entity.common.EntitySheetHeadersConstant;
import com.aastrika.entity.common.MemoryUtil;
import com.aastrika.entity.config.EntitySheetProperties;
import com.aastrika.entity.dto.BatchProcessingResult;
import com.aastrika.entity.dto.EntitySheetRow;
import com.aastrika.entity.exception.HeaderMissingException;
import com.aastrika.entity.exception.SheetDataMissingException;
import com.aastrika.entity.exception.UploadEntityException;
import com.aastrika.entity.mapper.MasterEntityMapper;
import com.aastrika.entity.model.MasterEntity;
import com.aastrika.entity.repository.jpa.MasterEntityRepository;
import com.aastrika.entity.util.SheetUtil;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.util.StringUtil;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
@Slf4j
@RequiredArgsConstructor
public class CsvEntitySheetReader implements EntitySheetReader {
  private final MasterEntityMapper masterEntityMapper;
  private final MasterEntityRepository masterEntityRepository;
  private final EntitySheetProperties entitySheetProperties;
  private final SheetUtil sheetUtil;

  private String globalEntityType;

  private void setGlobalEntityType(String globalEntityType) {
    this.globalEntityType = globalEntityType;
  }

  @Override
  public String getGlobalEntityType() {
    return this.globalEntityType;
  }

  /**
   * @param entitySheet
   * @return
   */
  public Map<String, List<EntitySheetRow>> getCompiledEntitySheet(MultipartFile entitySheet) {
    log.info("File name: {}", entitySheet.getOriginalFilename());
    MemoryUtil.logMemoryUsage("Before parsing CSV");

    Map<String, List<EntitySheetRow>> entitySheetMap = new HashMap<>();

    CSVFormat csvFormat = CSVFormat.DEFAULT
      .builder()
      .setHeader()
      .setSkipHeaderRecord(true)
      .setTrim(true)
      .setIgnoreEmptyLines(true)
      .build();

    Map<String, Integer> headerMap = new HashMap<>();

    try (InputStreamReader inputStreamReader =
            new InputStreamReader(entitySheet.getInputStream(), StandardCharsets.UTF_8);
        BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
        CSVParser csvParser = new CSVParser(bufferedReader, csvFormat)) {

      List<CSVRecord> csvRecords = csvParser.getRecords();

      String entityType = getValidatedSheetType(csvParser, csvRecords);
      validateMissingData(csvRecords, entityType);
      setGlobalEntityType(entityType);

      List<EntitySheetRow> entitySheetRows = sheetUtil.mapSheetToEntitySheetRow(csvRecords);
      entitySheetMap.put(entityType.toUpperCase(), entitySheetRows);

      log.info("Record size: {}", csvRecords.size());
      MemoryUtil.logMemoryUsage("After creating CSVParser");

    } catch (UploadEntityException e) {
      throw new UploadEntityException(HttpStatus.BAD_REQUEST, "Error reading CSV file ", headerMap);
    } catch (IllegalArgumentException e) {
      throw new UploadEntityException(
          HttpStatus.BAD_REQUEST, e.getMessage(), e.getLocalizedMessage());
    } catch (IOException e) {
      throw new UploadEntityException(
          HttpStatus.INTERNAL_SERVER_ERROR, "Error reading CSV file", e.getMessage());
    }

    return entitySheetMap;
  }

  /**
   * @param csvParser
   * @param csvRecords
   * @return
   */
  private @NonNull String getValidatedSheetType(CSVParser csvParser, List<CSVRecord> csvRecords) {
    List<String> headerList = csvParser.getHeaderNames();

    if (csvRecords == null || csvRecords.isEmpty()) {
      throw new SheetDataMissingException(HttpStatus.BAD_REQUEST, "There is no data in the sheet");
    }

    if (headerList == null || headerList.isEmpty()) {
      throw new HeaderMissingException(HttpStatus.BAD_REQUEST, "There is no header in the sheet");
    }

    if (!headerList.containsAll(entitySheetProperties.getHeaders().getRequired())) {
      List<String> remainingHeaderList =
        entitySheetProperties.getHeaders().getRequired().stream()
          .filter(header -> !headerList.contains(header))
          .toList();

      throw new HeaderMissingException(HttpStatus.BAD_REQUEST, "Missing required attributes",
        Map.of("missingAttribute", remainingHeaderList));
    }

    Set<String> entityTypeSet =
      csvRecords.stream()
        .map(csvRecord -> csvRecord.get(EntitySheetHeadersConstant.ENTITY_TYPE))
        .filter(Objects::nonNull)
        .collect(Collectors.toSet());

    String entityType = entityTypeSet.stream()
      .findFirst()
      .orElseThrow(() -> new SheetDataMissingException(
        HttpStatus.BAD_REQUEST, "Entity type not present"));

    if (entityTypeSet.size() > 1) {
      throw new SheetDataMissingException(
        HttpStatus.BAD_REQUEST, "Invalid entity type - missing or more than one entity type in sheet");
    }

    if (EntitySheetHeadersConstant.COMPETENCY_TYPE.equalsIgnoreCase(entityType)) {
      if (!headerList.containsAll(entitySheetProperties.getHeaders().getCompetencyLevels())) {
        List<String> remainingCompetencyHeaderList = entitySheetProperties.getHeaders().getCompetencyLevels().stream()
            .filter(header -> !headerList.contains(header))
            .toList();

        throw new HeaderMissingException(HttpStatus.BAD_REQUEST, "Missing competency attributes",
          Map.of("missingAttribute", remainingCompetencyHeaderList));
      }
    }

    return entityType;
  }

  /**
   * @param csvRecords
   * @param entityType
   */
  private void validateMissingData(@NonNull List<CSVRecord> csvRecords, @NonNull String entityType) {
    Map<String, List<Integer>> missingDataMap = new HashMap<>();
    boolean isCompetencyType = EntitySheetHeadersConstant.COMPETENCY_TYPE.equalsIgnoreCase(entityType);

    List<String> requiredFields = List.of(
      EntitySheetHeadersConstant.ENTITY_CODE,
      EntitySheetHeadersConstant.ENTITY_TYPE,
      EntitySheetHeadersConstant.NAME,
      EntitySheetHeadersConstant.DESCRIPTION
    );

    List<String> competencyFields = List.of(
      EntitySheetHeadersConstant.COMPETENCY_LEVEL_1_NAME,
      EntitySheetHeadersConstant.COMPETENCY_LEVEL_1_DESCRIPTION,
      EntitySheetHeadersConstant.COMPETENCY_LEVEL_2_NAME,
      EntitySheetHeadersConstant.COMPETENCY_LEVEL_2_DESCRIPTION,
      EntitySheetHeadersConstant.COMPETENCY_LEVEL_3_NAME,
      EntitySheetHeadersConstant.COMPETENCY_LEVEL_3_DESCRIPTION,
      EntitySheetHeadersConstant.COMPETENCY_LEVEL_4_NAME,
      EntitySheetHeadersConstant.COMPETENCY_LEVEL_4_DESCRIPTION,
      EntitySheetHeadersConstant.COMPETENCY_LEVEL_5_NAME,
      EntitySheetHeadersConstant.COMPETENCY_LEVEL_5_DESCRIPTION
    );

    for (CSVRecord csvRecord : csvRecords) {
      int rowNumber = (int) csvRecord.getRecordNumber();

      for (String field : requiredFields) {
        if (StringUtil.isBlank(csvRecord.get(field)))
          missingDataMap.computeIfAbsent(field, key -> new ArrayList<>()).add(rowNumber + 1);
      }

      if (isCompetencyType) {
        for (String field : competencyFields) {
          if (StringUtil.isBlank(csvRecord.get(field)))
            missingDataMap.computeIfAbsent(field, key -> new ArrayList<>()).add(rowNumber + 1);
        }
      }
    }
    if (!missingDataMap.isEmpty()) {
      throw new SheetDataMissingException(HttpStatus.BAD_REQUEST, "Data is missing in sheet", missingDataMap);
    }
  }

  /** Batch processing with error tracking */
  public BatchProcessingResult readAndSaveInBatches(MultipartFile file, int batchSize) {
    log.info(
        "Reading CSV file in batches: {}, batchSize: {}", file.getOriginalFilename(), batchSize);
    MemoryUtil.logMemoryUsage("Before batch processing");

    BatchProcessingResult result =
        BatchProcessingResult.builder().totalRecords(0).successCount(0).failedCount(0).build();

    CSVFormat csvFormat =
        CSVFormat.DEFAULT
            .builder()
            .setHeader()
            .setSkipHeaderRecord(true)
            .setTrim(true)
            .setIgnoreEmptyLines(true)
            .build();

    try (BufferedReader bufferedReader =
            new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));
        CSVParser csvParser = new CSVParser(bufferedReader, csvFormat)) {

      List<EntitySheetRow> batch = new ArrayList<>(batchSize);
      int rowIndex = 0;
      int batchNumber = 0;

      for (CSVRecord record : csvParser) {
        rowIndex++;
        result.setTotalRecords(rowIndex);

        try {
          EntitySheetRow row = mapRecordToRow(record);
          batch.add(row);

          // Process batch when full
          if (batch.size() >= batchSize) {
            batchNumber++;
            processBatch(batch, batchNumber, result);
            batch = new ArrayList<>(batchSize); // Clear for next batch
            MemoryUtil.logMemoryUsage("After batch " + batchNumber);
          }
        } catch (Exception e) {
          log.error("Error parsing row {}: {}", rowIndex, e.getMessage());
          result.addError(batchNumber, rowIndex, "Parse error: " + e.getMessage());
        }
      }

      // Process remaining records
      if (!batch.isEmpty()) {
        batchNumber++;
        processBatch(batch, batchNumber, result);
        MemoryUtil.logMemoryUsage("After final batch " + batchNumber);
      }

    } catch (Exception e) {
      log.error("Error reading CSV file: {}", e.getMessage());
      throw new RuntimeException("Error reading CSV file", e);
    }

    log.info(
        "Batch processing complete. Total: {}, Success: {}, Failed: {}",
        result.getTotalRecords(),
        result.getSuccessCount(),
        result.getFailedCount());
    MemoryUtil.logMemoryUsage("After batch processing complete");

    return result;
  }

  private void processBatch(
      List<EntitySheetRow> batch, int batchNumber, BatchProcessingResult result) {
    log.info("Processing batch {} with {} records", batchNumber, batch.size());

    try {
      List<MasterEntity> entities = getMasterEntityList(batch);
      masterEntityRepository.saveAll(entities);

      result.incrementSuccess(batch.size());
      result.setLastSuccessfulBatch(batchNumber);
      result.setLastSuccessfulRowIndex(result.getSuccessCount());

      log.info("Batch {} saved successfully", batchNumber);
    } catch (Exception e) {
      log.error("Error saving batch {}: {}", batchNumber, e.getMessage());

      // Mark all records in this batch as failed
      int startIndex = (batchNumber - 1) * batch.size() + 1;
      for (int i = 0; i < batch.size(); i++) {
        result.addError(batchNumber, startIndex + i, "DB save error: " + e.getMessage());
      }
    }
  }

  private List<MasterEntity> getMasterEntityList(List<EntitySheetRow> entitySheetRows) {
    if (entitySheetRows != null && !entitySheetRows.isEmpty()) {
      return entitySheetRows.stream()
          .map(masterEntityMapper::toEntity)
          .collect(Collectors.toList());
    }

    return Collections.EMPTY_LIST;
  }

  @Override
  public boolean supports(String contentType, String fileName) {
    return "text/csv".equals(contentType)
        || (fileName != null && fileName.toLowerCase().endsWith(".csv"));
  }

  private EntitySheetRow mapRecordToRow(CSVRecord record) {
    return EntitySheetRow.builder()
        .entityId(getValueOrNull(record, EntitySheetHeadersConstant.ENTITY_ID))
        .type(getValueOrNull(record, EntitySheetHeadersConstant.TYPE))
        .name(getValueOrNull(record, EntitySheetHeadersConstant.NAME))
        .description(getValueOrNull(record, EntitySheetHeadersConstant.DESCRIPTION))
        .language(getValueOrNull(record, EntitySheetHeadersConstant.LANGUAGE))
        .code(getValueOrNull(record, EntitySheetHeadersConstant.ENTITY_CODE))
        .levelId(getValueOrNull(record, EntitySheetHeadersConstant.LEVEL_ID))
        .createdBy(getValueOrNull(record, EntitySheetHeadersConstant.CREATED_BY))
        .updatedBy(getValueOrNull(record, EntitySheetHeadersConstant.UPDATED_BY))
        .reviewedBy(getValueOrNull(record, EntitySheetHeadersConstant.REVIEWED_BY))
        //                .createdDate(new Date())
        //                .updatedDate(getValueOrNull(record, EntitySheetHeaders.UPDATED_DATE))
        .reviewedDate(getValueOrNull(record, EntitySheetHeadersConstant.REVIEWED_DATE))
        //                .additionalProperties(getValueOrNull(record,
        // EntitySheetHeaders.ADDITIONAL_PROPERTIES))
        .competencyLevel1Name(getValueOrNull(record, EntitySheetHeadersConstant.COMPETENCY_LEVEL_1_NAME))
        .competencyLevel1Description(
            getValueOrNull(record, EntitySheetHeadersConstant.COMPETENCY_LEVEL_1_DESCRIPTION))
        .competencyLevel2Name(getValueOrNull(record, EntitySheetHeadersConstant.COMPETENCY_LEVEL_2_NAME))
        .competencyLevel2Description(
            getValueOrNull(record, EntitySheetHeadersConstant.COMPETENCY_LEVEL_2_DESCRIPTION))
        .competencyLevel3Name(getValueOrNull(record, EntitySheetHeadersConstant.COMPETENCY_LEVEL_3_NAME))
        .competencyLevel3Description(
            getValueOrNull(record, EntitySheetHeadersConstant.COMPETENCY_LEVEL_3_DESCRIPTION))
        .competencyLevel4Name(getValueOrNull(record, EntitySheetHeadersConstant.COMPETENCY_LEVEL_4_NAME))
        .competencyLevel4Description(
            getValueOrNull(record, EntitySheetHeadersConstant.COMPETENCY_LEVEL_4_DESCRIPTION))
        .competencyLevel5Name(getValueOrNull(record, EntitySheetHeadersConstant.COMPETENCY_LEVEL_5_NAME))
        .competencyLevel5Description(
            getValueOrNull(record, EntitySheetHeadersConstant.COMPETENCY_LEVEL_5_DESCRIPTION))
        .build();
  }

  private String getValueOrNull(CSVRecord record, String header) {
    if (record.isMapped(header)) {
      String value = record.get(header);
      return (value != null && !value.isBlank()) ? value : null;
    }
    return null;
  }
}
