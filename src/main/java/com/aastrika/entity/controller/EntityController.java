package com.aastrika.entity.controller;

import com.aastrika.entity.document.MasterEntityDocument;
import com.aastrika.entity.dto.response.ApiResponse;
import com.aastrika.entity.model.MasterEntity;
import com.aastrika.entity.service.MasterEntityEsService;
import com.aastrika.entity.service.MasterEntityService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/master-entities")
@RequiredArgsConstructor
public class EntityController {

  private final MasterEntityService masterEntityService;
  private final MasterEntityEsService masterEntityEsService;

  @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<ApiResponse> uploadEntitySheet(
      @RequestParam("entitySheet") MultipartFile entitySheet) {
    ApiResponse apiResponse = masterEntityService.processAndUploadSheet(entitySheet);

    return ResponseEntity.status(HttpStatus.CREATED).body(apiResponse);
  }

  /** Test endpoint - supports CSV and XLSX using EntitySheetReaderFactory */
  //    @PostMapping(value = "/upload/v2", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  //    public ResponseEntity<List<EntitySheetRow>> uploadEntitySheetV2(@RequestParam("file")
  // MultipartFile file) {
  //        List<EntitySheetRow> rows = masterEntityService.parseEntitySheetV2(file);
  //        return ResponseEntity.ok(rows);
  //    }

  /** Create a new entity */
  @PostMapping
  public ResponseEntity<MasterEntity> create(@RequestBody MasterEntity entity) {
    MasterEntity created = masterEntityService.create(entity);
    return ResponseEntity.status(HttpStatus.CREATED).body(created);
  }

  /** Get all entities */
  @GetMapping
  public ResponseEntity<List<MasterEntity>> getAll() {
    List<MasterEntity> entities = masterEntityService.findAll();
    return ResponseEntity.ok(entities);
  }

  /** Fuzzy search by name - handles typos and misspellings */
  @GetMapping("/search/name/fuzzy")
  public ResponseEntity<List<MasterEntityDocument>> fuzzySearchByName(
      @RequestParam("name") String name) {
    List<MasterEntityDocument> results = masterEntityEsService.fuzzyPhraseSearchByName(name);
    return ResponseEntity.ok(results);
  }

  /** Get entities by type */
  @GetMapping("/type/{type}")
  public ResponseEntity<List<MasterEntity>> getByEntityType(@PathVariable String type) {
    List<MasterEntity> entities = masterEntityService.findByEntityType(type);
    return ResponseEntity.ok(entities);
  }

  /** Get entity by code - keep this LAST (generic path variable) */
  @GetMapping("/code/{code}")
  public ResponseEntity<List<MasterEntity>> getByCode(@PathVariable String code) {
    List<MasterEntity> entities = masterEntityService.findByCode(code);
    return ResponseEntity.ok(entities);
  }
}
