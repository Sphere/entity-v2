package com.aastrika.entity.support;

import com.aastrika.entity.config.EntitySheetProperties;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

/**
 * Reads the real {@code application.properties} from the test classpath.
 *
 * <p>Unit tests that need configuration bind it from the deployed file rather than hardcoding
 * values, so renaming a CSV column header or adding an entity type cannot leave the tests
 * asserting against config that no longer exists.
 */
public final class TestApplicationProperties {

  private static final String PROPERTIES_FILE = "application.properties";

  private TestApplicationProperties() {}

  public static Properties load() {
    Properties properties = new Properties();
    try (InputStream in = Objects.requireNonNull(
        TestApplicationProperties.class.getClassLoader().getResourceAsStream(PROPERTIES_FILE),
        PROPERTIES_FILE + " not found on the test classpath")) {
      properties.load(in);
    } catch (IOException e) {
      throw new IllegalStateException("Failed to read " + PROPERTIES_FILE, e);
    }
    return properties;
  }

  /**
   * Binds the {@code entity-sheet.*} block the same way Spring Boot does at runtime, so the
   * required/optional/competency header lists and the header→field mappings under test are
   * exactly the deployed ones.
   */
  public static EntitySheetProperties entitySheetProperties() {
    Map<String, Object> source = new HashMap<>();
    load().forEach((key, value) -> source.put((String) key, value));

    return new Binder(new MapConfigurationPropertySource(source))
        .bind("entity-sheet", Bindable.of(EntitySheetProperties.class))
        .orElseThrow(() -> new IllegalStateException(
            "No entity-sheet.* properties found in " + PROPERTIES_FILE));
  }
}
