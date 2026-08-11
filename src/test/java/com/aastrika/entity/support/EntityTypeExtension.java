package com.aastrika.entity.support;

import com.aastrika.entity.enums.EntityType;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * Registers the valid entity types into {@link EntityType} for plain unit tests.
 *
 * <p>In a running application this happens once at startup via EntityStartupApplicationRunner.
 * Mockito-only tests never boot Spring, so the type set stays empty and every
 * {@code EntityType.validate(...)} call at a service boundary fails with
 * "Invalid entityType: '...'. Allowed values: []".
 *
 * <p>Types are read from the real {@code application.properties} on the classpath rather than
 * hardcoded here, so the set used by tests cannot drift from the deployed one when
 * {@code entity.entityTypeList} changes.
 */
public class EntityTypeExtension implements BeforeAllCallback {

  private static final String PROPERTIES_FILE = "application.properties";
  private static final String ENTITY_TYPE_LIST = "entity.entityTypeList";

  @Override
  public void beforeAll(ExtensionContext context) {
    loadEntityTypes();
  }

  /**
   * Loads {@code entity.entityTypeList} into {@link EntityType}. Safe to call repeatedly —
   * {@code EntityType.load} only adds to its type set.
   */
  public static void loadEntityTypes() {
    String value = TestApplicationProperties.load().getProperty(ENTITY_TYPE_LIST);
    if (value == null || value.isBlank()) {
      throw new IllegalStateException(
          ENTITY_TYPE_LIST + " is missing or empty in " + PROPERTIES_FILE);
    }

    List<String> types = Arrays.stream(value.split(","))
        .map(String::trim)
        .filter(s -> !s.isBlank())
        .toList();

    EntityType.load(types);
  }
}
