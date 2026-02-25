package com.aastrika.entity.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum EntityType {

  COMPETENCY,
  ROLE,
  ACTIVITY,
  POSITION;

  @JsonValue
  public String getValue() {
    return this.name();
  }

  @JsonCreator
  public static EntityType fromValue(String value) {
    if (value == null) return null;
    try {
      return EntityType.valueOf(value.toUpperCase());
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException("Invalid entityType: '" + value + "'. Allowed values: COMPETENCY, ROLE, ACTIVITY, POSITION");
    }
  }
}
