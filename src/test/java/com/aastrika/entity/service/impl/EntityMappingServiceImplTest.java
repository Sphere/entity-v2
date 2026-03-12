package com.aastrika.entity.service.impl;

import static com.aastrika.entity.support.TestDataLoader.competencyLevels;
import static com.aastrika.entity.support.TestDataLoader.entityMap;
import static com.aastrika.entity.support.TestDataLoader.masterEntity;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aastrika.entity.dto.request.CompetencyLevelDTO;
import com.aastrika.entity.dto.request.EntityMappingRequestDTO;
import com.aastrika.entity.dto.request.EntitySearchRequestDTO;
import com.aastrika.entity.dto.response.EntityChildHierarchyDTO;
import com.aastrika.entity.dto.response.EntityMappingResponseDTO;
import com.aastrika.entity.dto.response.FullHierarchyNodeDTO;
import com.aastrika.entity.dto.response.HierarchyResponseDTO;
import com.aastrika.entity.enums.EntityType;
import com.aastrika.entity.exception.MissingMappingDataException;
import com.aastrika.entity.exception.UpdateEntityException;
import com.aastrika.entity.mapper.CompetencyLevelMapper;
import com.aastrika.entity.mapper.EntityMapMapper;
import com.aastrika.entity.model.CompetencyLevel;
import com.aastrika.entity.model.EntityMap;
import com.aastrika.entity.model.MasterEntity;
import com.aastrika.entity.repository.jpa.EntityMapRepository;
import com.aastrika.entity.repository.jpa.MasterEntityRepository;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class EntityMappingServiceImplTest {

  @Mock private EntityMapRepository entityMapRepository;
  @Mock private MasterEntityRepository masterEntityRepository;
  @Mock private EntityMapMapper entityMapMapper;
  @Mock private CompetencyLevelMapper competencyLevelMapper;

  @InjectMocks private EntityMappingServiceImpl entityMappingService;

  // ─── Test data from CSV fixtures ─────────────────────────────────────────────

  private final MasterEntity P1  = masterEntity("P1");
  private final MasterEntity R1  = masterEntity("R1");
  private final MasterEntity R2  = masterEntity("R2");
  private final MasterEntity A1  = masterEntity("A1");
  private final MasterEntity A2  = masterEntity("A2");
  private final MasterEntity A3  = masterEntity("A3");
  private final MasterEntity C25 = masterEntity("C25");
  private final MasterEntity C26 = masterEntity("C26");

  private final EntityMap P1_R1  = entityMap("P1", "R1");
  private final EntityMap P1_R2  = entityMap("P1", "R2");
  private final EntityMap R1_A1  = entityMap("R1", "A1");
  private final EntityMap R1_A2  = entityMap("R1", "A2");
  private final EntityMap R1_A3  = entityMap("R1", "A3");
  private final EntityMap R2_A2  = entityMap("R2", "A2");
  private final EntityMap R2_A3  = entityMap("R2", "A3");
  private final EntityMap A1_C25 = entityMap("A1", "C25");
  private final EntityMap A2_C26 = entityMap("A2", "C26");

  private final List<CompetencyLevel>    c25Levels    = competencyLevels(EntityType.COMPETENCY, "C25");
  private final List<CompetencyLevel>    c26Levels    = competencyLevels(EntityType.COMPETENCY, "C26");
  private final List<CompetencyLevelDTO> c25LevelDTOs = toDTO(c25Levels);
  private final List<CompetencyLevelDTO> c26LevelDTOs = toDTO(c26Levels);

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(entityMappingService, "allowedTypeCombinations",
        Set.of("POSITION_ROLE", "ROLE_ACTIVITY", "ACTIVITY_COMPETENCY"));
  }

  // ─── saveEntityMapping ───────────────────────────────────────────────────────

  @Test
  @DisplayName("saveEntityMapping - should return empty list when input is null")
  void shouldReturnEmptyListWhenInputIsNull() {
    List<EntityMappingResponseDTO> result = entityMappingService.saveEntityMapping(null);
    assertNotNull(result);
    assertEquals(0, result.size());
  }

  @Test
  @DisplayName("saveEntityMapping - should return empty list when input is empty")
  void shouldReturnEmptyListWhenInputIsEmpty() {
    List<EntityMappingResponseDTO> result = entityMappingService.saveEntityMapping(List.of());
    assertNotNull(result);
    assertEquals(0, result.size());
  }

  @Test
  @DisplayName("saveEntityMapping - should throw BAD_REQUEST for invalid type combination")
  void shouldThrowForInvalidTypeCombination() {
    EntityMappingRequestDTO dto = new EntityMappingRequestDTO();
    dto.setParentEntityType(EntityType.POSITION);
    dto.setParentEntityCode("P1");
    dto.setChildEntityType(EntityType.COMPETENCY);  // POSITION_COMPETENCY not allowed
    dto.setChildEntityCode("C25");

    UpdateEntityException ex = assertThrows(UpdateEntityException.class,
        () -> entityMappingService.saveEntityMapping(List.of(dto)));

    assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    verify(masterEntityRepository, never()).findByCodeAndEntityType(any(), any());
  }

  @Test
  @DisplayName("saveEntityMapping - should throw NOT_FOUND when parent entity does not exist")
  void shouldThrowWhenParentEntityNotFound() {
    EntityMappingRequestDTO dto = new EntityMappingRequestDTO();
    dto.setParentEntityType(EntityType.POSITION);
    dto.setParentEntityCode("P1");
    dto.setChildEntityType(EntityType.ROLE);
    dto.setChildEntityCode("R1");

    when(masterEntityRepository.findByCodeAndEntityType("P1", EntityType.POSITION))
        .thenReturn(List.of());

    UpdateEntityException ex = assertThrows(UpdateEntityException.class,
        () -> entityMappingService.saveEntityMapping(List.of(dto)));

    assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    verify(entityMapRepository, never()).saveAll(anyList());
  }

  @Test
  @DisplayName("saveEntityMapping - should throw NOT_FOUND when child entity does not exist")
  void shouldThrowWhenChildEntityNotFound() {
    EntityMappingRequestDTO dto = new EntityMappingRequestDTO();
    dto.setParentEntityType(EntityType.POSITION);
    dto.setParentEntityCode("P1");
    dto.setChildEntityType(EntityType.ROLE);
    dto.setChildEntityCode("R1");

    when(masterEntityRepository.findByCodeAndEntityType("P1", EntityType.POSITION))
        .thenReturn(List.of(P1));
    when(masterEntityRepository.findByCodeAndEntityType("R1", EntityType.ROLE))
        .thenReturn(List.of());

    UpdateEntityException ex = assertThrows(UpdateEntityException.class,
        () -> entityMappingService.saveEntityMapping(List.of(dto)));

    assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    verify(entityMapRepository, never()).saveAll(anyList());
  }

  @Test
  @DisplayName("saveEntityMapping - should delete existing mappings then save new ones")
  void shouldDeleteExistingMappingsAndSaveNew() {
    EntityMappingRequestDTO dto = new EntityMappingRequestDTO();
    dto.setParentEntityType(EntityType.POSITION);
    dto.setParentEntityCode("P1");
    dto.setChildEntityType(EntityType.ROLE);
    dto.setChildEntityCode("R1");

    EntityMap existingMap = EntityMap.builder().id(10).parentEntityCode("P1")
        .parentEntityType(EntityType.POSITION).childEntityCode("R1")
        .childEntityType(EntityType.ROLE).build();
    EntityMap newMap = EntityMap.builder().parentEntityCode("P1")
        .parentEntityType(EntityType.POSITION).childEntityCode("R1")
        .childEntityType(EntityType.ROLE).build();

    when(masterEntityRepository.findByCodeAndEntityType("P1", EntityType.POSITION)).thenReturn(List.of(P1));
    when(masterEntityRepository.findByCodeAndEntityType("R1", EntityType.ROLE)).thenReturn(List.of(R1));
    when(entityMapRepository.findByParentEntityCodeAndParentEntityType("P1", EntityType.POSITION))
        .thenReturn(List.of(existingMap));
    when(entityMapMapper.toEntity(any(EntityMappingRequestDTO.class))).thenReturn(newMap);
    when(entityMapRepository.saveAll(anyList())).thenReturn(List.of(newMap));
    when(entityMapMapper.toResponseDTOList(anyList())).thenReturn(List.of(new EntityMappingResponseDTO()));

    List<EntityMappingResponseDTO> result = entityMappingService.saveEntityMapping(List.of(dto));

    assertNotNull(result);
    assertEquals(1, result.size());
    verify(entityMapRepository, times(1)).deleteAllById(List.of(10));
    verify(entityMapRepository, times(1)).flush();
    verify(entityMapRepository, times(1)).saveAll(anyList());
  }

  @Test
  @DisplayName("saveEntityMapping - should save directly without delete when no existing mappings")
  void shouldSaveDirectlyWhenNoExistingMappings() {
    EntityMappingRequestDTO dto = new EntityMappingRequestDTO();
    dto.setParentEntityType(EntityType.POSITION);
    dto.setParentEntityCode("P1");
    dto.setChildEntityType(EntityType.ROLE);
    dto.setChildEntityCode("R1");

    EntityMap newMap = EntityMap.builder().parentEntityCode("P1").childEntityCode("R1").build();

    when(masterEntityRepository.findByCodeAndEntityType("P1", EntityType.POSITION)).thenReturn(List.of(P1));
    when(masterEntityRepository.findByCodeAndEntityType("R1", EntityType.ROLE)).thenReturn(List.of(R1));
    when(entityMapRepository.findByParentEntityCodeAndParentEntityType("P1", EntityType.POSITION))
        .thenReturn(List.of());
    when(entityMapMapper.toEntity(any(EntityMappingRequestDTO.class))).thenReturn(newMap);
    when(entityMapRepository.saveAll(anyList())).thenReturn(List.of(newMap));
    when(entityMapMapper.toResponseDTOList(anyList())).thenReturn(List.of(new EntityMappingResponseDTO()));

    List<EntityMappingResponseDTO> result = entityMappingService.saveEntityMapping(List.of(dto));

    assertNotNull(result);
    assertEquals(1, result.size());
    verify(entityMapRepository, never()).deleteAllById(anyList());
    verify(entityMapRepository, never()).flush();
    verify(entityMapRepository, times(1)).saveAll(anyList());
  }

  @Test
  @DisplayName("saveEntityMapping - should set competency level list when child type is COMPETENCY")
  void shouldSetCompetencyLevelListWhenChildIsCompetency() {
    EntityMappingRequestDTO dto = new EntityMappingRequestDTO();
    dto.setParentEntityType(EntityType.ACTIVITY);
    dto.setParentEntityCode("A1");
    dto.setChildEntityType(EntityType.COMPETENCY);
    dto.setChildEntityCode("C25");
    dto.setCompetencies(List.of(1, 3, 5));

    EntityMap mappedEntityMap = EntityMap.builder().parentEntityCode("A1")
        .parentEntityType(EntityType.ACTIVITY).childEntityCode("C25")
        .childEntityType(EntityType.COMPETENCY).build();

    when(masterEntityRepository.findByCodeAndEntityType("A1", EntityType.ACTIVITY)).thenReturn(List.of(A1));
    when(masterEntityRepository.findByCodeAndEntityType("C25", EntityType.COMPETENCY)).thenReturn(List.of(C25));
    when(entityMapRepository.findByParentEntityCodeAndParentEntityType("A1", EntityType.ACTIVITY))
        .thenReturn(List.of());
    when(entityMapMapper.toEntity(any(EntityMappingRequestDTO.class))).thenReturn(mappedEntityMap);
    when(entityMapRepository.saveAll(anyList())).thenReturn(List.of(mappedEntityMap));
    when(entityMapMapper.toResponseDTOList(anyList())).thenReturn(List.of(new EntityMappingResponseDTO()));

    entityMappingService.saveEntityMapping(List.of(dto));

    assertEquals("1,3,5", mappedEntityMap.getCompetencyLevelList(),
        "Competency level list should be set as comma-separated string on the EntityMap");
  }

  // ─── getFullHierarchy ────────────────────────────────────────────────────────

  @Test
  @DisplayName("getFullHierarchy — P1 → R1/R2 → A1/A2/A3 → C25/C26")
  void shouldReturnFullHierarchyForP1() {
    givenMocks();

    FullHierarchyNodeDTO p1Node = entityMappingService.getFullHierarchy(
        buildRequest("p1", EntityType.POSITION, "en"));

    // P1
    assertNode(p1Node, P1);
    assertChildCount(p1Node, 2);
    FullHierarchyNodeDTO r1Node = p1Node.getChildren().get(0);
    FullHierarchyNodeDTO r2Node = p1Node.getChildren().get(1);

    // ├── R1
    assertNode(r1Node, R1);
    assertChildCount(r1Node, 3);
    FullHierarchyNodeDTO a1Node = r1Node.getChildren().get(0);
    FullHierarchyNodeDTO a2Node = r1Node.getChildren().get(1);
    FullHierarchyNodeDTO a3Node = r1Node.getChildren().get(2);

    // │   ├── A1 → C25
    assertNode(a1Node, A1);
    assertChildCount(a1Node, 1);
    assertCompetencyLeaf(a1Node.getChildren().get(0), C25, c25LevelDTOs);

    // │   ├── A2 → C26
    assertNode(a2Node, A2);
    assertChildCount(a2Node, 1);
    assertCompetencyLeaf(a2Node.getChildren().get(0), C26, c26LevelDTOs);

    // │   └── A3 (no children)
    assertNode(a3Node, A3);
    assertNull(a3Node.getChildren());

    // └── R2
    assertNode(r2Node, R2);
    assertChildCount(r2Node, 2);
    FullHierarchyNodeDTO r2a2Node = r2Node.getChildren().get(0);
    FullHierarchyNodeDTO r2a3Node = r2Node.getChildren().get(1);

    assertNode(r2a2Node, A2);
    assertChildCount(r2a2Node, 1);
    assertCompetencyLeaf(r2a2Node.getChildren().get(0), C26, c26LevelDTOs);

    assertNode(r2a3Node, A3);
    assertNull(r2a3Node.getChildren());
  }

  @Test
  @DisplayName("getFullHierarchy - should return leaf node when root has no children")
  void shouldReturnLeafNodeWhenRootHasNoChildren() {
    when(entityMapRepository.findByParentEntityCodeIn(argThat(l -> l != null && l.contains("A3"))))
        .thenReturn(List.of());
    when(masterEntityRepository.findByCodeInAndLanguageCode(anyList(), eq("en")))
        .thenReturn(List.of(A3));

    FullHierarchyNodeDTO node = entityMappingService.getFullHierarchy(
        buildRequest("A3", EntityType.ACTIVITY, "en"));

    assertAll(
        () -> assertNotNull(node),
        () -> assertEquals("A3", node.getEntityCode()),
        () -> assertNull(node.getChildren(), "Leaf node should have no children"),
        () -> assertNull(node.getCompetencies(), "Non-competency leaf should have no competencies")
    );
  }

  @Test
  @DisplayName("getFullHierarchy - should fallback to English for nodes missing in preferred language")
  void shouldFallbackToEnglishForMissingNodesInPreferredLanguage() {
    MasterEntity P1_fr = MasterEntity.builder()
        .code("P1").entityType(EntityType.POSITION).name("ANM (FR)").languageCode("fr").build();

    when(entityMapRepository.findByParentEntityCodeIn(argThat(l -> l != null && l.contains("P1"))))
        .thenReturn(List.of(P1_R1, P1_R2));
    when(entityMapRepository.findByParentEntityCodeIn(argThat(l -> l != null && l.containsAll(List.of("R1", "R2")))))
        .thenReturn(List.of());

    // P1 found in French, R1/R2 only available in English
    when(masterEntityRepository.findByCodeInAndLanguageCode(anyList(), eq("fr")))
        .thenReturn(List.of(P1_fr));
    when(masterEntityRepository.findByCodeInAndLanguageCode(anyList(), eq("en")))
        .thenReturn(List.of(R1, R2));

    FullHierarchyNodeDTO p1Node = entityMappingService.getFullHierarchy(
        buildRequest("P1", EntityType.POSITION, "fr"));

    assertAll(
        () -> assertNotNull(p1Node),
        () -> assertEquals("ANM (FR)", p1Node.getEntityName(), "Root should be in French"),
        () -> assertEquals("fr", p1Node.getLanguage()),
        () -> assertChildCount(p1Node, 2),
        () -> assertEquals("en", p1Node.getChildren().get(0).getLanguage(),
            "Missing French node should fall back to English"),
        () -> assertEquals("en", p1Node.getChildren().get(1).getLanguage(),
            "Missing French node should fall back to English")
    );
  }

  // ─── getEntityMappingHierarchy ───────────────────────────────────────────────

  @Test
  @DisplayName("getEntityMappingHierarchy - should return parent with non-competency children")
  void shouldReturnHierarchyWithNonCompetencyChildren() {
    when(entityMapRepository.findByParentEntityCodeAndParentEntityType("P1", EntityType.POSITION))
        .thenReturn(List.of(P1_R1, P1_R2));
    when(masterEntityRepository.findByCodeAndLanguageCode("P1", "en"))
        .thenReturn(Optional.of(P1));
    when(masterEntityRepository.findByCodeInAndLanguageCode(anyList(), eq("en")))
        .thenReturn(List.of(R1, R2));

    List<HierarchyResponseDTO> result = entityMappingService.getEntityMappingHierarchy(
        buildRequest("P1", EntityType.POSITION, "en"));

    assertAll(
        () -> assertNotNull(result),
        () -> assertEquals(1, result.size()),
        () -> assertEquals("P1", result.get(0).getEntityCode()),
        () -> assertEquals(EntityType.POSITION.name(), result.get(0).getEntityType()),
        () -> assertEquals(2, result.get(0).getChildHierarchy().size())
    );

    EntityChildHierarchyDTO child1 = result.get(0).getChildHierarchy().get(0);
    EntityChildHierarchyDTO child2 = result.get(0).getChildHierarchy().get(1);
    assertAll(
        () -> assertEquals("R1", child1.getEntityCode()),
        () -> assertEquals(EntityType.ROLE.name(), child1.getEntityType()),
        () -> assertNull(child1.getCompetencies(), "Non-competency child should have no competencies"),
        () -> assertEquals("R2", child2.getEntityCode())
    );
  }

  @Test
  @DisplayName("getEntityMappingHierarchy - should filter and attach competency levels for competency children")
  void shouldReturnHierarchyWithCompetencyChildren() {
    C25.setCompetencyLevels(c25Levels);

    when(entityMapRepository.findByParentEntityCodeAndParentEntityType("A1", EntityType.ACTIVITY))
        .thenReturn(List.of(A1_C25));
    when(masterEntityRepository.findByCodeAndLanguageCode("A1", "en"))
        .thenReturn(Optional.of(A1));
    when(masterEntityRepository.findByCodeInAndLanguageCode(anyList(), eq("en")))
        .thenReturn(List.of(C25));
    when(competencyLevelMapper.toCompetencyLevelDTOList(anyList()))
        .thenReturn(c25LevelDTOs);

    List<HierarchyResponseDTO> result = entityMappingService.getEntityMappingHierarchy(
        buildRequest("A1", EntityType.ACTIVITY, "en"));

    assertNotNull(result);
    assertEquals(1, result.size());

    HierarchyResponseDTO hierarchy = result.get(0);
    assertEquals("A1", hierarchy.getEntityCode());
    assertEquals(1, hierarchy.getChildHierarchy().size());

    EntityChildHierarchyDTO competencyChild = hierarchy.getChildHierarchy().get(0);
    assertAll(
        () -> assertEquals("C25", competencyChild.getEntityCode()),
        () -> assertEquals(EntityType.COMPETENCY.name(), competencyChild.getEntityType()),
        () -> assertNotNull(competencyChild.getCompetencies()),
        () -> assertEquals(c25LevelDTOs.size(), competencyChild.getCompetencies().size())
    );
  }

  @Test
  @DisplayName("getEntityMappingHierarchy - should throw BAD_REQUEST when parent entity not found")
  void shouldThrowWhenParentEntityNotFoundInHierarchy() {
    when(entityMapRepository.findByParentEntityCodeAndParentEntityType("P1", EntityType.POSITION))
        .thenReturn(List.of(P1_R1));
    when(masterEntityRepository.findByCodeAndLanguageCode("P1", "en"))
        .thenReturn(Optional.empty());

    MissingMappingDataException ex = assertThrows(MissingMappingDataException.class,
        () -> entityMappingService.getEntityMappingHierarchy(
            buildRequest("P1", EntityType.POSITION, "en")));

    assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    verify(masterEntityRepository, never()).findByCodeInAndLanguageCode(anyList(), any());
  }

  @Test
  @DisplayName("getEntityMappingHierarchy - should return empty child hierarchy when no mappings exist")
  void shouldReturnEmptyChildHierarchyWhenNoMappings() {
    when(entityMapRepository.findByParentEntityCodeAndParentEntityType("P1", EntityType.POSITION))
        .thenReturn(List.of());
    when(masterEntityRepository.findByCodeAndLanguageCode("P1", "en"))
        .thenReturn(Optional.of(P1));
    when(masterEntityRepository.findByCodeInAndLanguageCode(anyList(), eq("en")))
        .thenReturn(List.of());

    List<HierarchyResponseDTO> result = entityMappingService.getEntityMappingHierarchy(
        buildRequest("P1", EntityType.POSITION, "en"));

    assertAll(
        () -> assertNotNull(result),
        () -> assertEquals(1, result.size()),
        () -> assertEquals("P1", result.get(0).getEntityCode()),
        () -> assertNotNull(result.get(0).getChildHierarchy()),
        () -> assertEquals(0, result.get(0).getChildHierarchy().size())
    );
  }

  // ─── Mock setup ──────────────────────────────────────────────────────────────

  private void givenMocks() {
    C25.setCompetencyLevels(c25Levels);
    C26.setCompetencyLevels(c26Levels);

    when(entityMapRepository.findByParentEntityCodeIn(argThat(l -> l != null && l.contains("P1"))))
        .thenReturn(List.of(P1_R1, P1_R2));
    when(entityMapRepository.findByParentEntityCodeIn(argThat(l -> l != null && l.containsAll(List.of("R1", "R2")))))
        .thenReturn(List.of(R1_A1, R1_A2, R1_A3, R2_A2, R2_A3));
    when(entityMapRepository.findByParentEntityCodeIn(argThat(l -> l != null && l.containsAll(List.of("A1", "A2", "A3")))))
        .thenReturn(List.of(A1_C25, A2_C26));
    when(entityMapRepository.findByParentEntityCodeIn(argThat(l -> l != null && l.containsAll(List.of("C25", "C26")))))
        .thenReturn(List.of());
    when(masterEntityRepository.findByCodeInAndLanguageCode(anyList(), eq("en")))
        .thenReturn(List.of(P1, R1, R2, A1, A2, A3, C25, C26));
    when(competencyLevelMapper.toCompetencyLevelDTOList(c25Levels)).thenReturn(c25LevelDTOs);
    when(competencyLevelMapper.toCompetencyLevelDTOList(c26Levels)).thenReturn(c26LevelDTOs);
  }

  // ─── Assertion helpers ───────────────────────────────────────────────────────

  private void assertNode(FullHierarchyNodeDTO node, MasterEntity expected) {
    assertNotNull(node);
    assertEquals(expected.getCode(), node.getEntityCode());
    assertEquals(expected.getEntityType().name(), node.getEntityType());
    assertEquals(expected.getName(), node.getEntityName());
    assertEquals(expected.getLanguageCode(), node.getLanguage());
  }

  private void assertChildCount(FullHierarchyNodeDTO node, int count) {
    assertNotNull(node.getChildren());
    assertEquals(count, node.getChildren().size());
  }

  private void assertCompetencyLeaf(FullHierarchyNodeDTO node, MasterEntity expected,
      List<CompetencyLevelDTO> expectedLevels) {
    assertNode(node, expected);
    assertNull(node.getChildren());
    assertNotNull(node.getCompetencies());
    assertEquals(expectedLevels.size(), node.getCompetencies().size());
    for (int i = 0; i < expectedLevels.size(); i++) {
      CompetencyLevelDTO exp = expectedLevels.get(i);
      CompetencyLevelDTO act = node.getCompetencies().get(i);
      assertEquals(exp.getLevelNumber(), act.getLevelNumber(), "levelNumber mismatch at index " + i);
      assertEquals(exp.getLevelName(), act.getLevelName(), "levelName mismatch at index " + i);
      assertEquals(exp.getLevelDescription(), act.getLevelDescription(), "levelDescription mismatch at index " + i);
    }
  }

  // ─── Other helpers ───────────────────────────────────────────────────────────

  private static EntitySearchRequestDTO buildRequest(String code, EntityType type, String language) {
    EntitySearchRequestDTO request = new EntitySearchRequestDTO();
    request.setEntityCode(code);
    request.setEntityType(type);
    request.setEntityLanguage(language);
    return request;
  }

  private static List<CompetencyLevelDTO> toDTO(List<CompetencyLevel> levels) {
    return levels.stream()
        .map(cl -> CompetencyLevelDTO.builder()
            .levelNumber(cl.getLevelNumber())
            .levelName(cl.getLevelName())
            .levelDescription(cl.getLevelDescription())
            .build())
        .toList();
  }
}