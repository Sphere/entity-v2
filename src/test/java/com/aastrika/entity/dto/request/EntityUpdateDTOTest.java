package com.aastrika.entity.dto.request;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class EntityUpdateDTOTest {

  @Test
  @DisplayName("setCode - should convert lowercase code to uppercase")
  void shouldNormalizeCodeToUpperCase() {
    EntityUpdateDTO dto = new EntityUpdateDTO();
    dto.setCode("ps001");
    assertEquals("PS001", dto.getCode());
  }

  @Test
  @DisplayName("setCode - should convert mixed-case code to uppercase")
  void shouldNormalizeMixedCaseCodeToUpperCase() {
    EntityUpdateDTO dto = new EntityUpdateDTO();
    dto.setCode("Ps001");
    assertEquals("PS001", dto.getCode());
  }

  @Test
  @DisplayName("setCode - should keep already uppercase code unchanged")
  void shouldKeepUpperCaseCodeUnchanged() {
    EntityUpdateDTO dto = new EntityUpdateDTO();
    dto.setCode("PS001");
    assertEquals("PS001", dto.getCode());
  }

  @Test
  @DisplayName("setCode - should handle null without throwing NullPointerException")
  void shouldHandleNullCodeGracefully() {
    EntityUpdateDTO dto = new EntityUpdateDTO();
    dto.setCode(null);
    assertNull(dto.getCode());
  }
}