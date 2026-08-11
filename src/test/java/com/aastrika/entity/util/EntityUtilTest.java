package com.aastrika.entity.util;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.aastrika.entity.config.EntitySheetProperties;
import com.aastrika.entity.dto.EntitySheetRow;
import com.aastrika.entity.model.CompetencyLevel;
import com.aastrika.entity.model.MasterEntity;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.NotReadablePropertyException;

class EntityUtilTest {

  private final EntitySheetProperties properties = new EntitySheetProperties();
  private final EntityUtil entityUtil = new EntityUtil(properties);

  private final MasterEntity parent = MasterEntity.builder()
      .code("C001").languageCode("en").entityType("COMPETENCY").build();

  // ─── getCompetencyListByEntity ───────────────────────────────────────────────

  @Test
  @DisplayName("getCompetencyListByEntity - should build one level per populated pair, with the parent back-reference")
  void shouldBuildAllFiveCompetencyLevels() {
    List<CompetencyLevel> levels = entityUtil.getCompetencyListByEntity(fullRow(), parent);

    assertAll(
        () -> assertEquals(5, levels.size()),
        () -> assertEquals(List.of(1, 2, 3, 4, 5),
            levels.stream().map(CompetencyLevel::getLevelNumber).toList()),
        () -> assertEquals(List.of("Basic", "Intermediate", "Proficient", "Advanced", "Expert"),
            levels.stream().map(CompetencyLevel::getLevelName).toList()),
        () -> assertEquals("L3 desc", levels.get(2).getLevelDescription()),
        () -> assertTrue(levels.stream().allMatch(level -> level.getMasterEntity() == parent),
            "every level points back at the entity being saved")
    );
    assertSame(parent, levels.get(0).getMasterEntity());
  }

  @Test
  @DisplayName("getCompetencyListByEntity - should keep the source level number when levels are skipped")
  void shouldPreserveLevelNumbersAcrossGaps() {
    EntitySheetRow row = EntitySheetRow.builder()
        .competencyLevel1Name("Basic").competencyLevel1Description("L1 desc")
        .competencyLevel4Name("Advanced").competencyLevel4Description("L4 desc")
        .build();

    List<CompetencyLevel> levels = entityUtil.getCompetencyListByEntity(row, parent);

    assertAll(
        () -> assertEquals(2, levels.size()),
        () -> assertEquals(1, levels.get(0).getLevelNumber()),
        () -> assertEquals(4, levels.get(1).getLevelNumber(),
            "the level number comes from the column, not the list position")
    );
  }

  @Test
  @DisplayName("getCompetencyListByEntity - should skip a level unless both name and description are present")
  void shouldSkipPartiallyPopulatedLevels() {
    EntitySheetRow row = EntitySheetRow.builder()
        .competencyLevel1Name("Basic")                                  // description missing
        .competencyLevel2Description("L2 desc")                         // name missing
        .competencyLevel3Name("   ").competencyLevel3Description("L3")  // blank name
        .competencyLevel4Name("Advanced").competencyLevel4Description("  ") // blank description
        .competencyLevel5Name("Expert").competencyLevel5Description("L5 desc")
        .build();

    List<CompetencyLevel> levels = entityUtil.getCompetencyListByEntity(row, parent);

    assertAll(
        () -> assertEquals(1, levels.size(), "only level 5 has both halves"),
        () -> assertEquals(5, levels.get(0).getLevelNumber())
    );
  }

  @Test
  @DisplayName("getCompetencyListByEntity - should return an empty list for a row with no competency data")
  void shouldReturnEmptyListForRowWithoutCompetencyData() {
    EntitySheetRow row = EntitySheetRow.builder().code("R001").name("Developer").build();

    assertTrue(entityUtil.getCompetencyListByEntity(row, parent).isEmpty());
  }

  @Test
  @DisplayName("getCompetencyListByEntity - should honour entity-sheet.competency-level-size as the upper bound")
  void shouldStopAtConfiguredCompetencyLevelSize() {
    properties.setCompetencyLevelSize(2);

    List<CompetencyLevel> levels = entityUtil.getCompetencyListByEntity(fullRow(), parent);

    assertAll(
        () -> assertEquals(2, levels.size(), "levels 3 to 5 are not read when the size is 2"),
        () -> assertEquals(List.of(1, 2),
            levels.stream().map(CompetencyLevel::getLevelNumber).toList())
    );
  }

  @Test
  @DisplayName("getCompetencyListByEntity - should ignore a configured size beyond the five supported columns")
  void shouldIgnoreLevelsBeyondTheFifthColumn() {
    properties.setCompetencyLevelSize(7);

    List<CompetencyLevel> levels = entityUtil.getCompetencyListByEntity(fullRow(), parent);

    assertEquals(5, levels.size(),
        "EntitySheetRow only carries five levels, so 6 and 7 resolve to no data");
  }

  @Test
  @DisplayName("getCompetencyListByEntity - should accept a null parent entity")
  void shouldAllowNullParentEntity() {
    List<CompetencyLevel> levels = entityUtil.getCompetencyListByEntity(fullRow(), null);

    assertAll(
        () -> assertEquals(5, levels.size()),
        () -> assertNull(levels.get(0).getMasterEntity())
    );
  }

  // ─── getMissingDataDetails / buildMissedDataMap ───────────────────────────────

  /**
   * Documents current behaviour only. No caller in src/main invokes this method, and its body
   * never writes to the map — the {@code rowNumbers} local is assigned and discarded — so it
   * always returns an empty map regardless of input. If it is ever implemented, replace this
   * test rather than adjusting it.
   */
  @Test
  @DisplayName("getMissingDataDetails - always returns an empty map (unused, unimplemented)")
  void shouldAlwaysReturnEmptyMissingDataMap() {
    List<EntitySheetRow> rowsWithBlankIds = List.of(
        EntitySheetRow.builder().entityId("").code("R001").build(),
        EntitySheetRow.builder().code("R002").build());

    assertAll(
        () -> assertTrue(entityUtil.getMissingDataDetails(null).isEmpty()),
        () -> assertTrue(entityUtil.getMissingDataDetails(List.of()).isEmpty()),
        () -> assertTrue(entityUtil.getMissingDataDetails(rowsWithBlankIds).isEmpty(),
            "blank entity ids are detected but never recorded"),
        () -> assertTrue(entityUtil.getMissingDataDetails(
            List.of(EntitySheetRow.builder().entityId("E1").build())).isEmpty())
    );
  }

  /**
   * Documents current behaviour only. No caller in src/main invokes this method; it reads the
   * property and discards the value, leaving the supplied map untouched. Its one observable
   * effect is failing loudly on a field name that does not exist on EntitySheetRow.
   */
  @Test
  @DisplayName("buildMissedDataMap - leaves the map untouched and only validates the field name (unused)")
  void shouldLeaveMissedDataMapUntouched() {
    Map<String, List<Integer>> missedDataMap = new HashMap<>();
    EntitySheetRow row = EntitySheetRow.builder().code("R001").name("Developer").build();

    entityUtil.buildMissedDataMap(missedDataMap, "name", row);

    assertAll(
        () -> assertTrue(missedDataMap.isEmpty(), "nothing is ever added to the map"),
        () -> assertThrows(NotReadablePropertyException.class,
            () -> entityUtil.buildMissedDataMap(missedDataMap, "noSuchField", row),
            "an unknown field name fails rather than being ignored")
    );
  }

  // ─── Fixtures ────────────────────────────────────────────────────────────────

  private static EntitySheetRow fullRow() {
    return EntitySheetRow.builder()
        .code("C001")
        .competencyLevel1Name("Basic").competencyLevel1Description("L1 desc")
        .competencyLevel2Name("Intermediate").competencyLevel2Description("L2 desc")
        .competencyLevel3Name("Proficient").competencyLevel3Description("L3 desc")
        .competencyLevel4Name("Advanced").competencyLevel4Description("L4 desc")
        .competencyLevel5Name("Expert").competencyLevel5Description("L5 desc")
        .build();
  }
}
