package com.aastrika.entity.support;

import com.aastrika.entity.enums.EntityType;
import com.aastrika.entity.model.CompetencyLevel;
import com.aastrika.entity.model.EntityMap;
import com.aastrika.entity.model.MasterEntity;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

/**
 * Loads test fixture data from CSV files under src/test/resources/test_data/.
 */
public class TestDataLoader {

  private static final CSVFormat CSV_FORMAT = CSVFormat.Builder.create(CSVFormat.DEFAULT)
      .setHeader().setSkipHeaderRecord(true).build();

  private static final Map<String, MasterEntity> MASTER_ENTITIES = load("test_data/master_entities.csv", record -> {
    if (!"en".equals(record.get("language_code"))) return null;
    String code = record.get("code");
    return Map.entry(code, MasterEntity.builder()
        .code(code)
        .entityType(EntityType.valueOf(record.get("entity_type")))
        .name(record.get("name"))
        .description(record.get("description"))
        .languageCode(record.get("language_code"))
        .build());
  });

  private static final Map<String, EntityMap> ENTITY_MAPS = load("test_data/entity_map.csv", record -> {
    String parentCode     = record.get("parent_entity_code");
    String childCode      = record.get("child_entity_code");
    String competencyList = record.get("competency_list");
    return Map.entry(parentCode + "_" + childCode, EntityMap.builder()
        .parentEntityCode(parentCode)
        .parentEntityType(EntityType.valueOf(record.get("parent_entity_type")))
        .childEntityCode(childCode)
        .childEntityType(EntityType.valueOf(record.get("child_entity_type")))
        .competencyLevelList(competencyList.isBlank() ? null : competencyList)
        .build());
  });

  // key: "C25_3" (code + "_" + levelNumber)
  private static final Map<String, CompetencyLevel> COMPETENCY_LEVELS = load("test_data/competency_level.csv", record -> {
    if (!"en".equals(record.get("language_code"))) return null;
    String code = record.get("code");
    int levelNumber = Integer.parseInt(record.get("level_number"));
    return Map.entry(code + "_" + levelNumber, CompetencyLevel.builder()
        .levelNumber(levelNumber)
        .levelName(record.get("level_name"))
        .levelDescription(record.get("level_description"))
        .build());
  });

  private TestDataLoader() {}

  public static MasterEntity masterEntity(String code) {
    return MASTER_ENTITIES.get(code);
  }

  public static EntityMap entityMap(String parentCode, String childCode) {
    return ENTITY_MAPS.get(parentCode + "_" + childCode);
  }

  public static CompetencyLevel competencyLevel(String code, int levelNumber) {
    return COMPETENCY_LEVELS.get(code + "_" + levelNumber);
  }

  /**
   * Returns CompetencyLevel list for a given child entity code and type,
   * using the level numbers defined in entity_map.csv for that child.
   */
  public static List<CompetencyLevel> competencyLevels(EntityType childEntityType, String childEntityCode) {
    return ENTITY_MAPS.values().stream()
        .filter(em -> childEntityCode.equals(em.getChildEntityCode())
            && childEntityType == em.getChildEntityType()
            && em.getCompetencyLevelList() != null)
        .flatMap(em -> Arrays.stream(em.getCompetencyLevelList().split(","))
            .map(String::trim)
            .filter(s -> !s.isBlank())
            .map(Integer::parseInt)
            .map(level -> COMPETENCY_LEVELS.get(childEntityCode + "_" + level))
            .filter(Objects::nonNull))
        .toList();
  }

  // ─── Internal loader ─────────────────────────────────────────────────────────

  @FunctionalInterface
  private interface RecordMapper<V> {
    Map.Entry<String, V> map(CSVRecord record) throws Exception;
  }

  private static <V> Map<String, V> load(String resourcePath, RecordMapper<V> mapper) {
    Map<String, V> result = new HashMap<>();
    try (Reader reader = new InputStreamReader(
        Objects.requireNonNull(
            TestDataLoader.class.getClassLoader().getResourceAsStream(resourcePath),
            "Test resource not found: " + resourcePath),
        StandardCharsets.UTF_8)) {
      for (CSVRecord record : CSV_FORMAT.parse(reader)) {
        Map.Entry<String, V> entry = mapper.map(record);
        if (entry != null) {
          result.put(entry.getKey(), entry.getValue());
        }
      }
    } catch (Exception e) {
      throw new RuntimeException("Failed to load test data: " + resourcePath, e);
    }
    return result;
  }
}
