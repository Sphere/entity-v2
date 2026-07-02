package com.aastrika.entity.config;

import com.aastrika.entity.dto.EntitySheetRow;
import com.aastrika.entity.enums.EntityType;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConfigurationProperties(prefix = "entity")
@Slf4j
public class EntityStartupApplicationRunner implements ApplicationRunner {

  private final EntitySheetProperties entitySheetProperties;

  @Setter
  private List<String> entityTypeList = new ArrayList<>();

  /**
   * Runs at application startup and performs two pre-flight checks:
   *
   * 1. Entity type loading — reads entity.entityTypeList from application.properties and
   *    registers them into EntityType. Any entityType passed at runtime is validated
   *    against this set. To add a new generic type, update application.properties and redeploy.
   *
   * 2. Sheet mapping validation — verifies that every value in
   *    entity-sheet.header-field-mappings resolves to an actual field on EntitySheetRow.
   *    Fails fast to prevent a silent runtime crash when a CSV is uploaded with a
   *    misconfigured mapping.
   *
   *    Safe to change: the key (CSV column header).
   *    Must not change: the value (EntitySheetRow field name) — validated here.
   */
  @Override
  public void run(ApplicationArguments args) {
    EntityType.load(entityTypeList);
    log.info("Loaded entity types: {}", EntityType.getTypes());

    Map<String, String> mappings = entitySheetProperties.getHeaderFieldMappings();
    List<String> invalidMappings = new ArrayList<>();

    for (Map.Entry<String, String> entry : mappings.entrySet()) {
      String csvHeader = entry.getKey();
      String fieldName = entry.getValue();

      try {
        EntitySheetRow.class.getDeclaredField(fieldName);
      } catch (NoSuchFieldException e) {
        invalidMappings.add(String.format(
            "  '%s' -> '%s'  (field '%s' does not exist in EntitySheetRow)", csvHeader, fieldName, fieldName));
      }
    }

    if (!invalidMappings.isEmpty()) {
      throw new IllegalStateException(
          "Invalid entity-sheet.header-field-mappings in application.properties. "
              + "The following values do not match any field in EntitySheetRow:\n"
              + String.join("\n", invalidMappings));
    }

    log.info("EntitySheetRow mapping validation passed — {} mappings verified", mappings.size());
  }
}
