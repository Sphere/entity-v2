package com.aastrika.entity.mapper;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.aastrika.entity.dto.request.EntityMappingRequestDTO;
import com.aastrika.entity.dto.response.EntityMappingResponseDTO;
import com.aastrika.entity.model.EntityMap;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Exercises the MapStruct-generated EntityMapMapperImpl directly, including the
 * {@code toCompetencyList} default method that converts the stored comma-separated
 * competency_list column into a List&lt;Integer&gt;.
 */
class EntityMapMapperTest {

  private final EntityMapMapper mapper = new EntityMapMapperImpl();

  // ─── toEntity ────────────────────────────────────────────────────────────────

  @Test
  @DisplayName("toEntity - should map the four code/type fields and leave id and competencyLevelList unset")
  void shouldMapRequestDTOToEntity() {
    EntityMappingRequestDTO dto = new EntityMappingRequestDTO();
    dto.setParentEntityType("ACTIVITY");
    dto.setParentEntityCode("A1");
    dto.setChildEntityType("COMPETENCY");
    dto.setChildEntityCode("C25");
    dto.setCompetencies(List.of(1, 3, 5));

    EntityMap entityMap = mapper.toEntity(dto);

    assertAll(
        () -> assertEquals("ACTIVITY", entityMap.getParentEntityType()),
        () -> assertEquals("A1", entityMap.getParentEntityCode()),
        () -> assertEquals("COMPETENCY", entityMap.getChildEntityType()),
        () -> assertEquals("C25", entityMap.getChildEntityCode()),
        () -> assertNull(entityMap.getId(), "id is @Mapping(ignore) — assigned by the database"),
        // competencies on the DTO are NOT carried over by the mapper; EntityMappingServiceImpl
        // sets competencyLevelList explicitly after mapping.
        () -> assertNull(entityMap.getCompetencyLevelList(),
            "competencyLevelList is @Mapping(ignore) — the service sets it after mapping")
    );
  }

  @Test
  @DisplayName("toEntity - should return null for null input")
  void shouldReturnNullForNullRequestDTO() {
    assertNull(mapper.toEntity(null));
  }

  // ─── toResponseDTO ───────────────────────────────────────────────────────────

  @Test
  @DisplayName("toResponseDTO - should map fields and split competencyLevelList into integers")
  void shouldMapEntityToResponseDTO() {
    EntityMap entityMap = EntityMap.builder()
        .id(10)
        .parentEntityType("ACTIVITY")
        .parentEntityCode("A1")
        .childEntityType("COMPETENCY")
        .childEntityCode("C25")
        .competencyLevelList("1,3,5")
        .build();

    EntityMappingResponseDTO dto = mapper.toResponseDTO(entityMap);

    assertAll(
        () -> assertEquals("ACTIVITY", dto.getParentEntityType()),
        () -> assertEquals("A1", dto.getParentEntityCode()),
        () -> assertEquals("COMPETENCY", dto.getChildEntityType()),
        () -> assertEquals("C25", dto.getChildEntityCode()),
        () -> assertEquals(List.of(1, 3, 5), dto.getCompetencies())
    );
  }

  @Test
  @DisplayName("toResponseDTO - should return null for null input")
  void shouldReturnNullForNullEntityMap() {
    assertNull(mapper.toResponseDTO(null));
  }

  @Test
  @DisplayName("toResponseDTOList - should map every element, and null list to null")
  void shouldMapEntityMapList() {
    List<EntityMap> entityMaps = List.of(
        EntityMap.builder().parentEntityCode("A1").childEntityCode("C25").competencyLevelList("1").build(),
        EntityMap.builder().parentEntityCode("A2").childEntityCode("C26").competencyLevelList("").build());

    List<EntityMappingResponseDTO> dtos = mapper.toResponseDTOList(entityMaps);

    assertAll(
        () -> assertEquals(2, dtos.size()),
        () -> assertEquals("C25", dtos.get(0).getChildEntityCode()),
        () -> assertEquals(List.of(1), dtos.get(0).getCompetencies()),
        () -> assertEquals("C26", dtos.get(1).getChildEntityCode()),
        () -> assertTrue(dtos.get(1).getCompetencies().isEmpty()),
        () -> assertNull(mapper.toResponseDTOList(null)),
        () -> assertTrue(mapper.toResponseDTOList(List.of()).isEmpty())
    );
  }

  @Test
  @DisplayName("toResponseDTOList - should map null elements to null entries")
  void shouldMapNullElementsWithinList() {
    List<EntityMappingResponseDTO> dtos = mapper.toResponseDTOList(
        Arrays.asList(EntityMap.builder().childEntityCode("C25").build(), null));

    assertAll(
        () -> assertEquals(2, dtos.size()),
        () -> assertNotNull(dtos.get(0)),
        () -> assertNull(dtos.get(1))
    );
  }

  // ─── toCompetencyList ────────────────────────────────────────────────────────

  @Test
  @DisplayName("toCompetencyList - should return empty list for null and blank input")
  void shouldReturnEmptyListForNullAndBlank() {
    assertAll(
        () -> assertTrue(mapper.toCompetencyList(null).isEmpty()),
        () -> assertTrue(mapper.toCompetencyList("").isEmpty()),
        () -> assertTrue(mapper.toCompetencyList("   ").isEmpty())
    );
  }

  @Test
  @DisplayName("toCompetencyList - should parse a comma-separated series, trimming whitespace")
  void shouldParseCompetencySeries() {
    assertAll(
        () -> assertEquals(List.of(1), mapper.toCompetencyList("1")),
        () -> assertEquals(List.of(1, 3, 5), mapper.toCompetencyList("1,3,5")),
        () -> assertEquals(List.of(1, 3, 5), mapper.toCompetencyList(" 1 , 3 , 5 ")),
        () -> assertEquals(List.of(5, 1), mapper.toCompetencyList("5,1"), "order is preserved as stored"),
        () -> assertEquals(List.of(1, 3), mapper.toCompetencyList("1,3,"),
            "String.split discards trailing empty tokens, so a trailing comma is tolerated")
    );
  }

  /**
   * Documents current behaviour, which differs from
   * EntityMappingServiceImpl.convertStringifyCompetencyToInt — that method filters out
   * non-numeric and blank tokens with a regex, this one does not. The same
   * competency_list column is therefore parsed by two rules depending on the read path.
   */
  @Test
  @DisplayName("toCompetencyList - should throw on malformed input (no digit filtering, unlike the service)")
  void shouldThrowOnMalformedCompetencySeries() {
    assertAll(
        () -> assertThrows(NumberFormatException.class, () -> mapper.toCompetencyList("1,x,3")),
        () -> assertThrows(NumberFormatException.class, () -> mapper.toCompetencyList("1,,3"),
            "an empty token between commas is not skipped"),
        () -> assertThrows(NumberFormatException.class, () -> mapper.toCompetencyList(",1"),
            "a leading comma yields an empty token, which split does not discard"),
        () -> assertThrows(NumberFormatException.class, () -> mapper.toCompetencyList("1.5"),
            "decimals are rejected — only integers are valid level numbers")
    );
  }
}
