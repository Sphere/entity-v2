package com.aastrika.entity.mapper;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.aastrika.entity.dto.request.CompetencyLevelDTO;
import com.aastrika.entity.model.CompetencyLevel;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Exercises the MapStruct-generated CompetencyLevelMapperImpl directly.
 *
 * <p>Service tests mock this mapper, so a wrong or missing {@code @Mapping} cannot fail
 * anything there. Instantiating the generated impl is what pins the mapping down.
 */
class CompetencyLevelMapperTest {

  private final CompetencyLevelMapper mapper = new CompetencyLevelMapperImpl();

  @Test
  @DisplayName("toCompetencyLevelDTO - should map every field")
  void shouldMapCompetencyLevelToDTO() {
    CompetencyLevel level = CompetencyLevel.builder()
        .id(7)
        .levelNumber(3)
        .levelName("Proficient")
        .levelDescription("Works independently")
        .build();

    CompetencyLevelDTO dto = mapper.toCompetencyLevelDTO(level);

    assertAll(
        () -> assertNotNull(dto),
        () -> assertEquals(3, dto.getLevelNumber()),
        () -> assertEquals("Proficient", dto.getLevelName()),
        () -> assertEquals("Works independently", dto.getLevelDescription())
    );
  }

  @Test
  @DisplayName("toCompetencyLevelDTO - should return null for null input")
  void shouldReturnNullForNullCompetencyLevel() {
    assertNull(mapper.toCompetencyLevelDTO(null));
  }

  @Test
  @DisplayName("toCompetencyLevelDTOList - should map every element in order")
  void shouldMapCompetencyLevelList() {
    List<CompetencyLevel> levels = List.of(
        CompetencyLevel.builder().levelNumber(1).levelName("Basic").levelDescription("Beginner").build(),
        CompetencyLevel.builder().levelNumber(2).levelName("Intermediate").levelDescription("Guided").build());

    List<CompetencyLevelDTO> dtos = mapper.toCompetencyLevelDTOList(levels);

    assertAll(
        () -> assertEquals(2, dtos.size()),
        () -> assertEquals(1, dtos.get(0).getLevelNumber()),
        () -> assertEquals("Basic", dtos.get(0).getLevelName()),
        () -> assertEquals("Beginner", dtos.get(0).getLevelDescription()),
        () -> assertEquals(2, dtos.get(1).getLevelNumber()),
        () -> assertEquals("Intermediate", dtos.get(1).getLevelName()),
        () -> assertEquals("Guided", dtos.get(1).getLevelDescription())
    );
  }

  @Test
  @DisplayName("toCompetencyLevelDTOList - should map null elements to null entries")
  void shouldMapNullElementsWithinList() {
    List<CompetencyLevelDTO> dtos = mapper.toCompetencyLevelDTOList(
        Arrays.asList(CompetencyLevel.builder().levelNumber(1).build(), null));

    assertAll(
        () -> assertEquals(2, dtos.size()),
        () -> assertNotNull(dtos.get(0)),
        () -> assertNull(dtos.get(1), "a null element maps to a null entry, not an exception")
    );
  }

  @Test
  @DisplayName("toCompetencyLevelDTOList - should return null for null list and empty for empty list")
  void shouldHandleNullAndEmptyLists() {
    assertAll(
        () -> assertNull(mapper.toCompetencyLevelDTOList(null),
            "MapStruct returns null for a null list, not an empty list"),
        () -> assertTrue(mapper.toCompetencyLevelDTOList(List.of()).isEmpty())
    );
  }
}
