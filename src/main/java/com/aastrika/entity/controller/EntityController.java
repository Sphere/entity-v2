package com.aastrika.entity.controller;

import com.aastrika.entity.document.MasterEntityDocument;
import com.aastrika.entity.dto.request.EntityUpdateDTO;
import com.aastrika.entity.dto.request.SearchDTO;
import com.aastrika.entity.dto.response.ApiResponse;
import com.aastrika.entity.dto.response.EntityResponseDTO;
import com.aastrika.entity.model.MasterEntity;
import com.aastrika.entity.service.MasterEntityEsService;
import com.aastrika.entity.service.MasterEntityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
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
      @io.swagger.v3.oas.annotations.responses.ApiResponse(
          responseCode = "201",
          description = "Entities uploaded successfully",
          content = @Content(schema = @Schema(implementation = ApiResponse.class))),
      @io.swagger.v3.oas.annotations.responses.ApiResponse(
          responseCode = "400",
          description = "Invalid file or duplicate entries")
  })
  @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<ApiResponse> uploadEntitySheet(
      @Parameter(description = "Language code (e.g., en, hi)") @RequestParam("language") String language,
      @Parameter(description = "User ID performing the upload") @RequestParam("userId") String userId,
      @Parameter(description = "CSV or XLSX file") @RequestParam("entitySheet") MultipartFile entitySheet) {

    ApiResponse apiResponse = masterEntityService.processAndUploadSheet(entitySheet, userId);
    return ResponseEntity.status(HttpStatus.CREATED).body(apiResponse);
  }

  @Operation(
      summary = "Create a new entity",
      description = "Create a new master entity with all details")
  @ApiResponses(value = {
      @io.swagger.v3.oas.annotations.responses.ApiResponse(
          responseCode = "201",
          description = "Entity created successfully",
          content = @Content(schema = @Schema(implementation = MasterEntity.class))),
      @io.swagger.v3.oas.annotations.responses.ApiResponse(
          responseCode = "400",
          description = "Invalid entity data")
  })
  @PostMapping
  public ResponseEntity<MasterEntity> create(@RequestBody MasterEntity entity) {
    MasterEntity created = masterEntityService.create(entity);
    return ResponseEntity.status(HttpStatus.CREATED).body(created);
  }

  @Operation(
      summary = "Update an existing entity",
      description = "Update entity by code + languageCode. Only non-null fields will be updated.")
  @ApiResponses(value = {
      @io.swagger.v3.oas.annotations.responses.ApiResponse(
          responseCode = "200",
          description = "Entity updated successfully",
          content = @Content(schema = @Schema(implementation = EntityResponseDTO.class))),
      @io.swagger.v3.oas.annotations.responses.ApiResponse(
          responseCode = "400",
          description = "Entity not found or invalid data")
  })
  @PutMapping("/update")
  public ResponseEntity<ApiResponse> update(
      @RequestBody EntityUpdateDTO updateDTO,
      @Parameter(description = "User ID performing the update") @RequestParam("userId") String userId) {
    ApiResponse apiResponse = masterEntityService.update(updateDTO, userId);
    return ResponseEntity.status(HttpStatus.CREATED).body(apiResponse);
  }

  @Operation(
      summary = "Fuzzy search by name",
      description = "Search entities by name with typo tolerance. Handles misspellings and partial matches.")
  @ApiResponses(value = {
      @io.swagger.v3.oas.annotations.responses.ApiResponse(
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
      @io.swagger.v3.oas.annotations.responses.ApiResponse(
          responseCode = "200",
          description = "Search results returned",
          content = @Content(schema = @Schema(implementation = MasterEntityDocument.class)))
  })
  @PostMapping("/search")
  public ResponseEntity<List<MasterEntityDocument>> searchEntities(
      @RequestBody SearchDTO searchDTO) {
    List<MasterEntityDocument> results =
        masterEntityEsService.findEntitiesBySearchParameter(searchDTO);
    return ResponseEntity.ok(results);
  }
}
