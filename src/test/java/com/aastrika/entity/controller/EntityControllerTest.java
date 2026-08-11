package com.aastrika.entity.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.aastrika.entity.config.ApplicationExceptionHandler;
import com.aastrika.entity.document.MasterEntityDocument;
import com.aastrika.entity.dto.request.EntityCreateRequestDTO;
import com.aastrika.entity.dto.request.EntityDeleteRequestDTO;
import com.aastrika.entity.dto.request.EntityUpdateDTO;
import com.aastrika.entity.dto.request.SearchDTO;
import com.aastrika.entity.dto.response.AppResponse;
import com.aastrika.entity.dto.response.EntityResult;
import com.aastrika.entity.dto.response.MasterEntitySearchResponseDTO;
import com.aastrika.entity.exception.UpdateEntityException;
import com.aastrika.entity.exception.UploadEntityException;
import com.aastrika.entity.service.MasterEntityEsService;
import com.aastrika.entity.service.MasterEntityService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * Drives EntityController through the full Spring MVC pipeline — argument binding, {@code @Valid}
 * validation, message conversion and the real ApplicationExceptionHandler — without booting a
 * Spring context, so no database or OpenSearch is required.
 */
class EntityControllerTest {

  private MasterEntityService masterEntityService;
  private MasterEntityEsService masterEntityEsService;
  private MockMvc mockMvc;

  private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

  @BeforeEach
  void setUp() {
    masterEntityService = mock(MasterEntityService.class);
    masterEntityEsService = mock(MasterEntityEsService.class);

    mockMvc = MockMvcBuilders
        .standaloneSetup(new EntityController(masterEntityService, masterEntityEsService))
        .setControllerAdvice(new ApplicationExceptionHandler())
        .build();
  }

  // ─── POST /v1/entity/upload ──────────────────────────────────────────────────

  @Test
  @DisplayName("POST /upload - should return 201 and pass the file and userId to the service")
  void shouldUploadEntitySheet() throws Exception {
    MockMultipartFile file = new MockMultipartFile(
        "entitySheet", "entities.csv", "text/csv", "code,name\nR001,Developer".getBytes());
    when(masterEntityService.processAndUploadSheet(any(), eq("admin")))
        .thenReturn(AppResponse.success("api.entity.upload", EntityResult.empty(), HttpStatus.OK));

    mockMvc.perform(multipart("/v1/entity/upload")
            .file(file)
            .param("language", "en")
            .param("userId", "admin"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value("api.entity.upload"))
        .andExpect(jsonPath("$.ver").value("v1"))
        .andExpect(jsonPath("$.params.status").value("OK"));

    verify(masterEntityService).processAndUploadSheet(any(), eq("admin"));
  }

  @Test
  @DisplayName("POST /upload - should surface a service UploadEntityException as 400 with its payload")
  void shouldReturnBadRequestWhenUploadFails() throws Exception {
    MockMultipartFile file = new MockMultipartFile(
        "entitySheet", "entities.csv", "text/csv", "code\nR001".getBytes());
    when(masterEntityService.processAndUploadSheet(any(), anyString()))
        .thenThrow(new UploadEntityException(HttpStatus.BAD_REQUEST, "Duplicate entry found",
            Map.of("missingAttribute", List.of("name"))));

    mockMvc.perform(multipart("/v1/entity/upload")
            .file(file)
            .param("language", "en")
            .param("userId", "admin"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.params.errmsg").value("Duplicate entry found"))
        .andExpect(jsonPath("$.responseCode").value("Bad Request"))
        .andExpect(jsonPath("$.result.missingAttribute[0]").value("name"));
  }

  // ─── POST /v1/entity/create ──────────────────────────────────────────────────

  @Test
  @DisplayName("POST /create - should return 201 for a valid request")
  void shouldCreateEntity() throws Exception {
    EntityCreateRequestDTO request = new EntityCreateRequestDTO();
    request.setCode("R001");
    request.setLanguageCode("en");
    request.setEntityType("ROLE");
    request.setName("Developer");

    when(masterEntityService.create(any(EntityCreateRequestDTO.class), eq("admin")))
        .thenReturn(AppResponse.success("api.entity.create", EntityResult.empty(), HttpStatus.OK));

    mockMvc.perform(post("/v1/entity/create")
            .param("userId", "admin")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value("api.entity.create"));

    verify(masterEntityService).create(any(EntityCreateRequestDTO.class), eq("admin"));
  }

  @Test
  @DisplayName("POST /create - should return 400 naming each violated field when the body is invalid")
  void shouldRejectInvalidCreateRequest() throws Exception {
    // code and languageCode are both @NotBlank
    String body = "{\"entityType\":\"ROLE\",\"name\":\"Developer\"}";

    mockMvc.perform(post("/v1/entity/create")
            .param("userId", "admin")
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.params.errmsg").value(
            org.hamcrest.Matchers.containsString("Entity code must not be blank")))
        .andExpect(jsonPath("$.params.errmsg").value(
            org.hamcrest.Matchers.containsString("Entity language must not be blank")));

    verify(masterEntityService, org.mockito.Mockito.never())
        .create(any(EntityCreateRequestDTO.class), anyString());
  }

  @Test
  @DisplayName("POST /create - should map a service CONFLICT to 409")
  void shouldReturnConflictWhenEntityExists() throws Exception {
    EntityCreateRequestDTO request = new EntityCreateRequestDTO();
    request.setCode("R001");
    request.setLanguageCode("en");

    when(masterEntityService.create(any(EntityCreateRequestDTO.class), anyString()))
        .thenThrow(new UpdateEntityException(HttpStatus.CONFLICT, "Entity already exists"));

    mockMvc.perform(post("/v1/entity/create")
            .param("userId", "admin")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.params.errmsg").value("Entity already exists"));
  }

  // ─── PUT /v1/entity/update ───────────────────────────────────────────────────

  @Test
  @DisplayName("PUT /update - should return 200 for a valid list body")
  void shouldUpdateEntities() throws Exception {
    EntityUpdateDTO dto = new EntityUpdateDTO();
    dto.setCode("R001");
    dto.setLanguageCode("en");
    dto.setName("Updated");

    when(masterEntityService.update(anyList(), eq("admin")))
        .thenReturn(AppResponse.success("api.entity.update", EntityResult.empty(), HttpStatus.OK));

    mockMvc.perform(put("/v1/entity/update")
            .param("userId", "admin")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(List.of(dto))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value("api.entity.update"));

    verify(masterEntityService).update(anyList(), eq("admin"));
  }

  @Test
  @DisplayName("PUT /update - should map a service NOT_FOUND to 404")
  void shouldReturnNotFoundWhenUpdateTargetMissing() throws Exception {
    EntityUpdateDTO dto = new EntityUpdateDTO();
    dto.setCode("UNKNOWN");
    dto.setLanguageCode("en");

    when(masterEntityService.update(anyList(), anyString()))
        .thenThrow(new UpdateEntityException(HttpStatus.NOT_FOUND, "Entity not found for update"));

    mockMvc.perform(put("/v1/entity/update")
            .param("userId", "admin")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(List.of(dto))))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.params.errmsg").value("Entity not found for update"));
  }

  // ─── GET /v1/entity/search/name/fuzzy ────────────────────────────────────────

  @Test
  @DisplayName("GET /search/name/fuzzy - should return the documents the ES service found")
  void shouldFuzzySearchByName() throws Exception {
    when(masterEntityEsService.fuzzyPhraseSearchByName("Problm Solvng")).thenReturn(List.of(
        MasterEntityDocument.builder().id("PS001_en").code("PS001").name("Problem Solving").build()));

    mockMvc.perform(get("/v1/entity/search/name/fuzzy").param("name", "Problm Solvng"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].code").value("PS001"))
        .andExpect(jsonPath("$[0].name").value("Problem Solving"));

    verify(masterEntityEsService).fuzzyPhraseSearchByName("Problm Solvng");
  }

  @Test
  @DisplayName("GET /search/name/fuzzy - should return an empty array when nothing matches")
  void shouldReturnEmptyArrayWhenNoFuzzyMatch() throws Exception {
    when(masterEntityEsService.fuzzyPhraseSearchByName(anyString())).thenReturn(List.of());

    mockMvc.perform(get("/v1/entity/search/name/fuzzy").param("name", "zzz"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$").isEmpty());
  }

  // ─── POST /v1/entity/search ──────────────────────────────────────────────────

  @Test
  @DisplayName("POST /search - should return the wrapped search result")
  void shouldSearchEntities() throws Exception {
    SearchDTO searchDTO = new SearchDTO();
    searchDTO.setEntityType("ROLE");
    searchDTO.setLanguage("en");
    searchDTO.setQuery("developer");
    searchDTO.setField(List.of("name"));

    when(masterEntityEsService.findEntitiesBySearchParameter(any(SearchDTO.class)))
        .thenReturn(AppResponse.success("api.entity.search",
            EntityResult.of(List.of(MasterEntitySearchResponseDTO.builder()
                .code("R001").name("Developer").build())),
            HttpStatus.OK));

    mockMvc.perform(post("/v1/entity/search")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(searchDTO)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.result.count").value(1))
        .andExpect(jsonPath("$.result.entity[0].code").value("R001"));
  }

  /**
   * /search has no {@code @Valid}, so an unknown entityType reaches the service and comes back
   * as EntityType.validate's IllegalArgumentException, which the advice maps to 400.
   */
  @Test
  @DisplayName("POST /search - should map a leaked IllegalArgumentException to 400")
  void shouldMapIllegalArgumentToBadRequest() throws Exception {
    when(masterEntityEsService.findEntitiesBySearchParameter(any(SearchDTO.class)))
        .thenThrow(new IllegalArgumentException("Invalid entityType: 'BOGUS'. Allowed values: []"));

    mockMvc.perform(post("/v1/entity/search")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"entityType\":\"BOGUS\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.params.errmsg").value(
            org.hamcrest.Matchers.containsString("Invalid entityType: 'BOGUS'")));
  }

  // ─── DELETE /v1/entity/delete ────────────────────────────────────────────────

  @Test
  @DisplayName("DELETE /delete - should return 200 for a valid list body")
  void shouldDeleteEntities() throws Exception {
    EntityDeleteRequestDTO dto = new EntityDeleteRequestDTO();
    dto.setEntityCode("R001");
    dto.setEntityType("ROLE");
    dto.setLanguage("en");

    when(masterEntityService.deleteMasterEntities(anyList()))
        .thenReturn(AppResponse.success("api.entity.delete", EntityResult.empty(), HttpStatus.OK));

    mockMvc.perform(delete("/v1/entity/delete")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(List.of(dto))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value("api.entity.delete"));

    verify(masterEntityService).deleteMasterEntities(anyList());
  }

  // ─── Exception-handler coverage ──────────────────────────────────────────────

  @Test
  @DisplayName("handler - should map DataIntegrityViolationException to 409 using its root cause")
  void shouldMapDataIntegrityViolationToConflict() throws Exception {
    when(masterEntityService.deleteMasterEntities(anyList()))
        .thenThrow(new DataIntegrityViolationException("wrapper",
            new RuntimeException("violates foreign key constraint")));

    mockMvc.perform(delete("/v1/entity/delete")
            .contentType(MediaType.APPLICATION_JSON)
            .content("[{\"entityCode\":\"R001\",\"entityType\":\"ROLE\",\"language\":\"en\"}]"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.params.errmsg").value("violates foreign key constraint"));
  }

  @Test
  @DisplayName("handler - should fall back to a generic message when the violation has no root cause")
  void shouldUseGenericMessageWhenNoRootCause() throws Exception {
    when(masterEntityService.deleteMasterEntities(anyList()))
        .thenThrow(new DataIntegrityViolationException("no root cause here"));

    mockMvc.perform(delete("/v1/entity/delete")
            .contentType(MediaType.APPLICATION_JSON)
            .content("[{\"entityCode\":\"R001\",\"entityType\":\"ROLE\",\"language\":\"en\"}]"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.params.errmsg").value("Data integrity violation"));
  }

  @Test
  @DisplayName("handler - should map an unexpected exception to 500 under api.entity.error")
  void shouldMapUnexpectedExceptionToInternalServerError() throws Exception {
    when(masterEntityService.deleteMasterEntities(anyList()))
        .thenThrow(new IllegalStateException("connection pool exhausted"));

    mockMvc.perform(delete("/v1/entity/delete")
            .contentType(MediaType.APPLICATION_JSON)
            .content("[{\"entityCode\":\"R001\",\"entityType\":\"ROLE\",\"language\":\"en\"}]"))
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.id").value("api.entity.error"))
        .andExpect(jsonPath("$.params.errmsg").value("connection pool exhausted"));
  }

  /**
   * Documents current behaviour. ApplicationExceptionHandler declares a catch-all
   * {@code @ExceptionHandler(Exception.class)} and does not extend ResponseEntityExceptionHandler,
   * so Spring MVC's own request-binding failures are resolved by that catch-all and reported as
   * 500 rather than 400.
   */
  @Test
  @DisplayName("handler - framework binding failures are reported as 500, not 400")
  void shouldReportFrameworkBindingFailuresAsServerErrors() throws Exception {
    // userId is a required @RequestParam
    mockMvc.perform(post("/v1/entity/create")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"code\":\"R001\",\"languageCode\":\"en\"}"))
        .andExpect(status().isInternalServerError());

    // body is not parseable JSON
    mockMvc.perform(post("/v1/entity/create")
            .param("userId", "admin")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{not json"))
        .andExpect(status().isInternalServerError());
  }
}
