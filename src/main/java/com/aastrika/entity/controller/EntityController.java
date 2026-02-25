package com.aastrika.entity.controller;

import com.aastrika.entity.document.MasterEntityDocument;
import com.aastrika.entity.dto.request.EntityCreateRequestDTO;
import com.aastrika.entity.dto.request.EntityDeleteRequestDTO;
import com.aastrika.entity.dto.request.EntityUpdateDTO;
import com.aastrika.entity.dto.request.SearchDTO;
import com.aastrika.entity.dto.response.AppResponse;
import com.aastrika.entity.dto.response.EntityResponseDTO;
import com.aastrika.entity.dto.response.EntityResult;
import com.aastrika.entity.dto.response.MasterEntitySearchResponseDTO;
import com.aastrika.entity.model.MasterEntity;
import com.aastrika.entity.service.MasterEntityEsService;
import com.aastrika.entity.service.MasterEntityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/v1/entity")
@RequiredArgsConstructor
@Tag(name = "Entity Management", description = "APIs for managing master entities (Competencies, Roles, Activities)")
public class EntityController {

  private final MasterEntityService masterEntityService;
  private final MasterEntityEsService masterEntityEsService;

  @Operation(
      summary = "Upload entity sheet",
      description = "Upload CSV or XLSX file containing entity data (Competencies, Roles, etc.)")
  @ApiResponses(value = {
      @ApiResponse(
          responseCode = "201",
          description = "Entities uploaded successfully",
          content = @Content(schema = @Schema(implementation = AppResponse.class))),
      @ApiResponse(
          responseCode = "400",
          description = "Invalid file or duplicate entries")
  })
  @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<AppResponse> uploadEntitySheet(
      @Parameter(description = "Language code (e.g., en, hi)") @RequestParam("language") String language,
      @Parameter(description = "User ID performing the upload") @RequestParam("userId") String userId,
      @Parameter(description = "CSV or XLSX file") @RequestParam("entitySheet") MultipartFile entitySheet) {

    AppResponse appResponse = masterEntityService.processAndUploadSheet(entitySheet, userId);
    return ResponseEntity.status(HttpStatus.CREATED).body(appResponse);
  }

  @Operation(
      summary = "Create a new entity",
      description = "Create a new master entity with all details")
  @ApiResponses(value = {
      @ApiResponse(
          responseCode = "201",
          description = "Entity created successfully",
          content = @Content(schema = @Schema(implementation = MasterEntity.class))),
      @ApiResponse(
          responseCode = "400",
          description = "Invalid entity data")
  })
  @PostMapping("/create")
  public ResponseEntity<AppResponse> create(@RequestParam("userId") String userId,
                                            @RequestBody EntityCreateRequestDTO entityCreateRequestDTO) {
    AppResponse appResponse = masterEntityService.create(entityCreateRequestDTO, userId);
    return ResponseEntity.status(HttpStatus.CREATED).body(appResponse);
  }

  @Operation(
      summary = "Update an existing entity",
      description = "Update entity by code + languageCode. Only non-null fields will be updated.")
  @ApiResponses(value = {
      @ApiResponse(
          responseCode = "200",
          description = "Entity updated successfully",
          content = @Content(schema = @Schema(implementation = EntityResponseDTO.class))),
      @ApiResponse(
          responseCode = "400",
          description = "Entity not found or invalid data")
  })
  @PutMapping("/update")
  public ResponseEntity<AppResponse> update(
      @Valid @RequestBody EntityUpdateDTO updateDTO,
      @Parameter(description = "User ID performing the update") @RequestParam("userId") String userId) {
    AppResponse appResponse = masterEntityService.update(updateDTO, userId);
    return ResponseEntity.status(HttpStatus.CREATED).body(appResponse);
  }

  @Operation(
      summary = "Fuzzy search by name",
      description = "Search entities by name with typo tolerance. Handles misspellings and partial matches.")
  @ApiResponses(value = {
      @ApiResponse(
          responseCode = "200",
          description = "Search results returned",
          content = @Content(schema = @Schema(implementation = MasterEntityDocument.class)))
  })
  @GetMapping("/search/name/fuzzy")
  public ResponseEntity<List<MasterEntityDocument>> fuzzySearchByName(
      @Parameter(description = "Name to search for") @RequestParam("name") String name) {
    List<MasterEntityDocument> results = masterEntityEsService.fuzzyPhraseSearchByName(name);
    return ResponseEntity.ok(results);
  }

  @Operation(
      summary = "Search entities by parameters",
      description = "Dynamic search with filters on entityType, language, and custom fields. Supports both fuzzy and exact matching.")
  @ApiResponses(value = {
      @ApiResponse(
          responseCode = "200",
          description = "Search results returned",
          content = @Content(schema = @Schema(implementation = MasterEntityDocument.class)))
  })
  @PostMapping("/search")
  public ResponseEntity<AppResponse<EntityResult<MasterEntitySearchResponseDTO>>> searchEntities(
      @RequestBody SearchDTO searchDTO) {
    AppResponse<EntityResult<MasterEntitySearchResponseDTO>> results =
      masterEntityEsService.findEntitiesBySearchParameter(searchDTO);
    return ResponseEntity.ok(results);
  }

  @Operation(
      summary = "Delete entities",
      description = "Delete one or more master entities by entityCode, entityType and language. "
          + "Cascades deletion of entity mappings and competency levels.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Entities deleted successfully"),
      @ApiResponse(responseCode = "400", description = "Invalid request"),
      @ApiResponse(responseCode = "404", description = "Entity not found")
  })
  @DeleteMapping("/delete")
  public ResponseEntity<AppResponse> deleteEntities(
      @Valid @RequestBody List<EntityDeleteRequestDTO> deleteRequests) {
    AppResponse response = masterEntityService.deleteMasterEntities(deleteRequests);
    return ResponseEntity.ok(response);
  }
}
