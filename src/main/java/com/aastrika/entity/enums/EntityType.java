package com.aastrika.entity.enums;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class EntityType {

  public static final String COMPETENCY = "COMPETENCY";

  private static final Set<String> TYPES = new HashSet<>();

  private EntityType() {}

  public static void load(List<String> types) {
    types.forEach(t -> TYPES.add(t.toUpperCase()));
  }

  public static void validate(String value) {
    if (value == null || !TYPES.contains(value.toUpperCase())) {
      throw new IllegalArgumentException(
          "Invalid entityType: '" + value + "'. Allowed values: " + TYPES);
    }
  }

  public static boolean isValid(String value) {
    return value != null && TYPES.contains(value.toUpperCase());
  }

  public static Set<String> getTypes() {
    return Set.copyOf(TYPES);
  }
}
