package com.aastrika.entity.reader;

import com.aastrika.entity.common.ApplicationConstants;
import com.aastrika.entity.common.EntitySheetHeaders;
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
import org.springframework.http.HttpStatus;
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

  public void compileEntitySheet(MultipartFile entitySheet) {
    log.info("File name: {}", entitySheet.getOriginalFilename());
    MemoryUtil.logMemoryUsage("Before parsing CSV");

    CSVFormat csvFormat =
        CSVFormat.DEFAULT
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
      MemoryUtil.logMemoryUsage("After creating CSVParser");

      List<CSVRecord> csvRecords = csvParser.getRecords();
      String entityType =
          getValidatedSheetType(csvParser, csvRecords)
              .orElseThrow(
                  () ->
                      new SheetDataMissingException(
                          HttpStatus.BAD_REQUEST, "Entity type not present"));

      if (ApplicationConstants.COMPETENCY_TYPE.equalsIgnoreCase(entityType)) {}

      List<EntitySheetRow> entitySheetRows = sheetUtil.mapSheetToEntitySheetRow(csvRecords);
      List<MasterEntity> masterEntities =
          entitySheetRows.stream().map(masterEntityMapper::toEntity).toList();

      masterEntityRepository.saveAll(masterEntities);
      // Elastic search saving

      log.info("Record size: {}", csvRecords.size());
      log.info("Record number: {}", csvParser.getRecordNumber());

    } catch (UploadEntityException e) {
      throw new UploadEntityException(HttpStatus.BAD_REQUEST, "Error reading CSV file", headerMap);
    } catch (IllegalArgumentException e) {
      throw new UploadEntityException(
          HttpStatus.BAD_REQUEST, e.getMessage(), e.getLocalizedMessage());
    } catch (IOException e) {
      throw new UploadEntityException(
          HttpStatus.INTERNAL_SERVER_ERROR, "Error reading CSV file", e.getMessage());
    }
  }

  private Optional<String> getValidatedSheetType(CSVParser csvParser, List<CSVRecord> csvRecords) {
    List<String> headerList = csvParser.getHeaderNames();

    System.out.println("Header list: " + headerList);

    if (headerList == null || headerList.isEmpty()) {
      throw new HeaderMissingException(HttpStatus.BAD_REQUEST, "There is no header in the sheet");
    }

    if (!headerList.containsAll(entitySheetProperties.getHeaders().getRequired())) {
      List<String> remainingHeaderList =
          entitySheetProperties.getHeaders().getRequired().stream()
              .filter(header -> !headerList.contains(header))
              .toList();

      throw new HeaderMissingException(
          HttpStatus.BAD_REQUEST,
          "Missing required attribute",
          Map.of("missingAttribute", remainingHeaderList));
    }

    if (csvRecords == null || csvRecords.isEmpty()) {
      throw new SheetDataMissingException(HttpStatus.BAD_REQUEST, "There is no data in the sheet");
    }

    Set<String> entityTypeSet =
        csvRecords.stream()
            .map(csvRecord -> csvRecord.get(ApplicationConstants.ENTITY_TYPE))
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

    String entityType =
        entityTypeSet.stream()
            .findFirst()
            .orElseThrow(
                () ->
                    new SheetDataMissingException(
                        HttpStatus.BAD_REQUEST, "Entity type not present"));

    if (entityTypeSet.size() > 1) {
      throw new SheetDataMissingException(
          HttpStatus.BAD_REQUEST, "Invalid entity type - more than one entity type in sheet");
    }

    if (ApplicationConstants.COMPETENCY_TYPE.equalsIgnoreCase(entityType)) {
      if (!headerList.containsAll(entitySheetProperties.getHeaders().getCompetencyLevels())) {
        List<String> remainingCompetencyHeaderList =
            entitySheetProperties.getHeaders().getCompetencyLevels().stream()
                .filter(header -> !headerList.contains(header))
                .toList();

        throw new HeaderMissingException(
            HttpStatus.BAD_REQUEST,
            "Missing competency attribute",
            Map.of("missingAttribute", remainingCompetencyHeaderList));
      }
    }
    return entityTypeSet.stream().findFirst();
  }

  private EntitySheetRow getEntitySheetRow(CSVRecord csvRecord, String entityType) {
    EntitySheetRow entitySheetRow = new EntitySheetRow();

    List<String> predefinedHeaderList = entitySheetProperties.getHeaders().getRequired();

    for (String headerName : predefinedHeaderList) {
      entitySheetRow.setEntityId(getCellValueOrNull(csvRecord, headerName));
    }

    if (ApplicationConstants.COMPETENCY_TYPE.equalsIgnoreCase(entityType)) {}

    return entitySheetRow;
  }

  private String getCellValueOrNull(CSVRecord record, String header) {
    if (record.isMapped(header)) {
      String value = record.get(header);
      return (value != null && !value.isBlank()) ? value : null;
    }
    return null;
  }

  @Override
  public List<EntitySheetRow> read(MultipartFile file) {
    compileEntitySheet(file);
    if (true) return null;

    log.info("Reading CSV file: {}", file.getOriginalFilename());
    List<EntitySheetRow> entitySheetRows = new ArrayList<>();

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

      for (CSVRecord record : csvParser) {
        EntitySheetRow row = mapRecordToRow(record);
        entitySheetRows.add(row);
      }
    } catch (Exception e) {
      throw new RuntimeException("Error reading CSV file", e);
    }

    List<MasterEntity> masterEntities = getMasterEntityList(entitySheetRows);

    masterEntityRepository.saveAll(masterEntities);
    MemoryUtil.logMemoryUsage("After saving to DB");

    log.info("Read {} rows from CSV", entitySheetRows.size());
    return entitySheetRows;
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
        .entityId(getValueOrNull(record, EntitySheetHeaders.ENTITY_ID))
        .type(getValueOrNull(record, EntitySheetHeaders.TYPE))
        .name(getValueOrNull(record, EntitySheetHeaders.NAME))
        .description(getValueOrNull(record, EntitySheetHeaders.DESCRIPTION))
        .language(getValueOrNull(record, EntitySheetHeaders.LANGUAGE))
        .code(getValueOrNull(record, EntitySheetHeaders.CODE))
        .levelId(getValueOrNull(record, EntitySheetHeaders.LEVEL_ID))
        .createdBy(getValueOrNull(record, EntitySheetHeaders.CREATED_BY))
        .updatedBy(getValueOrNull(record, EntitySheetHeaders.UPDATED_BY))
        .reviewedBy(getValueOrNull(record, EntitySheetHeaders.REVIEWED_BY))
        //                .createdDate(new Date())
        //                .updatedDate(getValueOrNull(record, EntitySheetHeaders.UPDATED_DATE))
        .reviewedDate(getValueOrNull(record, EntitySheetHeaders.REVIEWED_DATE))
        //                .additionalProperties(getValueOrNull(record,
        // EntitySheetHeaders.ADDITIONAL_PROPERTIES))
        .competencyLevel1Name(getValueOrNull(record, EntitySheetHeaders.COMPETENCY_LEVEL_1_NAME))
        .competencyLevel1Description(
            getValueOrNull(record, EntitySheetHeaders.COMPETENCY_LEVEL_1_DESCRIPTION))
        .competencyLevel2Name(getValueOrNull(record, EntitySheetHeaders.COMPETENCY_LEVEL_2_NAME))
        .competencyLevel2Description(
            getValueOrNull(record, EntitySheetHeaders.COMPETENCY_LEVEL_2_DESCRIPTION))
        .competencyLevel3Name(getValueOrNull(record, EntitySheetHeaders.COMPETENCY_LEVEL_3_NAME))
        .competencyLevel3Description(
            getValueOrNull(record, EntitySheetHeaders.COMPETENCY_LEVEL_3_DESCRIPTION))
        .competencyLevel4Name(getValueOrNull(record, EntitySheetHeaders.COMPETENCY_LEVEL_4_NAME))
        .competencyLevel4Description(
            getValueOrNull(record, EntitySheetHeaders.COMPETENCY_LEVEL_4_DESCRIPTION))
        .competencyLevel5Name(getValueOrNull(record, EntitySheetHeaders.COMPETENCY_LEVEL_5_NAME))
        .competencyLevel5Description(
            getValueOrNull(record, EntitySheetHeaders.COMPETENCY_LEVEL_5_DESCRIPTION))
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
