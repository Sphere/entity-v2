package com.aastrika.entity.controller;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.aastrika.entity.config.ApplicationExceptionHandler;
import com.aastrika.entity.dto.request.EntityMappingRequestDTO;
import com.aastrika.entity.dto.request.EntitySearchRequestDTO;
import com.aastrika.entity.dto.response.EntityChildHierarchyDTO;
import com.aastrika.entity.dto.response.EntityMappingResponseDTO;
import com.aastrika.entity.dto.response.FullHierarchyNodeDTO;
import com.aastrika.entity.dto.response.HierarchyResponseDTO;
import com.aastrika.entity.exception.MissingMappingDataException;
import com.aastrika.entity.exception.UpdateEntityException;
import com.aastrika.entity.service.impl.EntityMappingServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * Drives EntityMappingController through the real MVC pipeline and exception advice.
 *
 * <p>The controller injects the concrete EntityMappingServiceImpl rather than an interface —
 * CLAUDE.md flags this as a pattern not to repeat — so the mock here is of the impl class.
 */
class EntityMappingControllerTest {

  private EntityMappingServiceImpl entityMappingService;
  private MockMvc mockMvc;

  private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

  @BeforeEach
  void setUp() {
    entityMappingService = mock(EntityMappingServiceImpl.class);

    mockMvc = MockMvcBuilders
        .standaloneSetup(new EntityMappingController(entityMappingService))
        .setControllerAdvice(new ApplicationExceptionHandler())
        .build();
  }

  // ─── POST /v1/entity/mapping ─────────────────────────────────────────────────

  @Test
  @DisplayName("POST /mapping - should return 201 wrapped under api.entity.mapping")
  void shouldSaveEntityMapping() throws Exception {
    EntityMappingResponseDTO saved = new EntityMappingResponseDTO();
    saved.setParentEntityType("ACTIVITY");
    saved.setParentEntityCode("A1");
    saved.setChildEntityType("COMPETENCY");
    saved.setChildEntityCode("C25");
    saved.setCompetencies(List.of(1, 3));

    when(entityMappingService.saveEntityMapping(anyList())).thenReturn(List.of(saved));

    mockMvc.perform(post("/v1/entity/mapping")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(List.of(request("ACTIVITY", "A1", "COMPETENCY", "C25")))))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value("api.entity.mapping"))
        .andExpect(jsonPath("$.responseCode").value("Created"))
        .andExpect(jsonPath("$.params.status").value("Created"))
        .andExpect(jsonPath("$.result[0].childEntityCode").value("C25"))
        .andExpect(jsonPath("$.result[0].competencies[1]").value(3));

    verify(entityMappingService).saveEntityMapping(anyList());
  }

  @Test
  @DisplayName("POST /mapping - should return 400 when a mapping in the list is missing required fields")
  void shouldRejectInvalidMappingRequest() throws Exception {
    String body = "[{\"parentEntityType\":\"ACTIVITY\",\"parentEntityCode\":\"A1\"}]";

    mockMvc.perform(post("/v1/entity/mapping")
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.params.errmsg").value(containsString("child entity type must not be blank")));

    verify(entityMappingService, never()).saveEntityMapping(anyList());
  }

  @Test
  @DisplayName("POST /mapping - should map a rejected type combination to 400")
  void shouldReturnBadRequestForInvalidTypeCombination() throws Exception {
    when(entityMappingService.saveEntityMapping(anyList()))
        .thenThrow(new UpdateEntityException(HttpStatus.BAD_REQUEST,
            "Invalid mapping structure: POSITION_COMPETENCY"));

    mockMvc.perform(post("/v1/entity/mapping")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(List.of(request("POSITION", "P1", "COMPETENCY", "C25")))))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.params.errmsg").value(containsString("Invalid mapping structure")));
  }

  @Test
  @DisplayName("POST /mapping - should return 201 with an empty result for an empty list")
  void shouldAcceptEmptyMappingList() throws Exception {
    when(entityMappingService.saveEntityMapping(anyList())).thenReturn(List.of());

    mockMvc.perform(post("/v1/entity/mapping")
            .contentType(MediaType.APPLICATION_JSON)
            .content("[]"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.result").isEmpty());
  }

  // ─── POST /v1/entity/mapping/search ──────────────────────────────────────────

  @Test
  @DisplayName("POST /mapping/search - should return 200 wrapped under api.entity.mapping.search")
  void shouldReturnMappingHierarchy() throws Exception {
    EntityChildHierarchyDTO child = new EntityChildHierarchyDTO();
    child.setEntityCode("R1");
    child.setEntityType("ROLE");
    child.setEntityName("Nurse");

    HierarchyResponseDTO hierarchy = HierarchyResponseDTO.builder()
        .entityCode("P1").entityType("POSITION").entityName("ANM").language("en")
        .childHierarchy(List.of(child))
        .build();

    when(entityMappingService.getEntityMappingHierarchy(any(EntitySearchRequestDTO.class)))
        .thenReturn(List.of(hierarchy));

    mockMvc.perform(post("/v1/entity/mapping/search")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(searchRequest("P1", "POSITION", "en"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value("api.entity.mapping.search"))
        .andExpect(jsonPath("$.result[0].entityCode").value("P1"))
        .andExpect(jsonPath("$.result[0].childHierarchy[0].entityCode").value("R1"));
  }

  @Test
  @DisplayName("POST /mapping/search - should return 400 when the search request is missing fields")
  void shouldRejectInvalidSearchRequest() throws Exception {
    mockMvc.perform(post("/v1/entity/mapping/search")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"entityCode\":\"P1\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.params.errmsg").value(containsString("Entity type must not be blank")))
        .andExpect(jsonPath("$.params.errmsg").value(containsString("Entity language must not be blank")));

    verify(entityMappingService, never()).getEntityMappingHierarchy(any());
  }

  @Test
  @DisplayName("POST /mapping/search - should map MissingMappingDataException to 400")
  void shouldReturnBadRequestWhenParentEntityMissing() throws Exception {
    when(entityMappingService.getEntityMappingHierarchy(any(EntitySearchRequestDTO.class)))
        .thenThrow(new MissingMappingDataException(HttpStatus.BAD_REQUEST, "Parent entity not found"));

    mockMvc.perform(post("/v1/entity/mapping/search")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(searchRequest("P1", "POSITION", "en"))))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.params.errmsg").value("Parent entity not found"));
  }

  // ─── POST /v1/entity/hierarchy ───────────────────────────────────────────────

  @Test
  @DisplayName("POST /hierarchy - should return the nested tree under api.entity.mapping.full-hierarchy")
  void shouldReturnFullHierarchy() throws Exception {
    FullHierarchyNodeDTO leaf = FullHierarchyNodeDTO.builder()
        .entityCode("R1").entityType("ROLE").entityName("Nurse").language("en").build();
    FullHierarchyNodeDTO root = FullHierarchyNodeDTO.builder()
        .entityCode("P1").entityType("POSITION").entityName("ANM").language("en")
        .children(List.of(leaf))
        .build();

    when(entityMappingService.getFullHierarchy(any(EntitySearchRequestDTO.class))).thenReturn(root);

    mockMvc.perform(post("/v1/entity/hierarchy")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(searchRequest("P1", "POSITION", "en"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value("api.entity.mapping.full-hierarchy"))
        .andExpect(jsonPath("$.result.entityCode").value("P1"))
        .andExpect(jsonPath("$.result.children[0].entityCode").value("R1"));
  }

  @Test
  @DisplayName("POST /hierarchy - should return 200 with a null result when the root is not found")
  void shouldReturnNullResultWhenRootMissing() throws Exception {
    when(entityMappingService.getFullHierarchy(any(EntitySearchRequestDTO.class))).thenReturn(null);

    mockMvc.perform(post("/v1/entity/hierarchy")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(searchRequest("NOPE", "POSITION", "en"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value("api.entity.mapping.full-hierarchy"))
        .andExpect(jsonPath("$.result").doesNotExist());
  }

  // ─── Helpers ─────────────────────────────────────────────────────────────────

  private static EntityMappingRequestDTO request(
      String parentType, String parentCode, String childType, String childCode) {
    EntityMappingRequestDTO dto = new EntityMappingRequestDTO();
    dto.setParentEntityType(parentType);
    dto.setParentEntityCode(parentCode);
    dto.setChildEntityType(childType);
    dto.setChildEntityCode(childCode);
    return dto;
  }

  private static EntitySearchRequestDTO searchRequest(String code, String type, String language) {
    EntitySearchRequestDTO dto = new EntitySearchRequestDTO();
    dto.setEntityCode(code);
    dto.setEntityType(type);
    dto.setEntityLanguage(language);
    return dto;
  }
}
