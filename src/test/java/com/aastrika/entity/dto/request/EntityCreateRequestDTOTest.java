package com.aastrika.entity.dto.request;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class EntityCreateRequestDTOTest {

  @Test
  @DisplayName("setCode - should convert lowercase code to uppercase")
  void shouldNormalizeCodeToUpperCase() {
    EntityCreateRequestDTO dto = new EntityCreateRequestDTO();
    dto.setCode("ps001");
    assertEquals("PS001", dto.getCode());
  }

  @Test
  @DisplayName("setCode - should convert mixed-case code to uppercase")
  void shouldNormalizeMixedCaseCodeToUpperCase() {
    EntityCreateRequestDTO dto = new EntityCreateRequestDTO();
    dto.setCode("Ps001");
    assertEquals("PS001", dto.getCode());
  }

  @Test
  @DisplayName("setCode - should keep already uppercase code unchanged")
  void shouldKeepUpperCaseCodeUnchanged() {
    EntityCreateRequestDTO dto = new EntityCreateRequestDTO();
    dto.setCode("PS001");
    assertEquals("PS001", dto.getCode());
  }

  @Test
  @DisplayName("setCode - should handle null without throwing NullPointerException")
  void shouldHandleNullCodeGracefully() {
    EntityCreateRequestDTO dto = new EntityCreateRequestDTO();
    dto.setCode(null);
    assertNull(dto.getCode());
  }
}