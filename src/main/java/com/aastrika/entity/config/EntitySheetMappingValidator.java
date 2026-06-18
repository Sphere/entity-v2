package com.aastrika.entity.config;

import com.aastrika.entity.dto.EntitySheetRow;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class EntitySheetMappingValidator implements ApplicationRunner {

  private final EntitySheetProperties entitySheetProperties;

  /**
   * Validates that every value in entity-sheet.header-field-mappings resolves to an actual field
   * on {@link EntitySheetRow}. Fails fast at startup to prevent a silent runtime crash when a CSV
   * is uploaded with a misconfigured mapping.
   *
   * Safe to change: the key (CSV column header).
   * Must not change: the value (EntitySheetRow field name) — validated here.
   */
  @Override
  public void run(ApplicationArguments args) {
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