package com.aastrika.entity.controller;

import com.aastrika.entity.dto.EntitySheetRow;
import com.aastrika.entity.dto.response.ApiResponse;
import com.aastrika.entity.model.MasterEntity;
import com.aastrika.entity.service.MasterEntityService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/master-entities")
@RequiredArgsConstructor
public class EntityController {

    private final MasterEntityService masterEntityService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse> uploadEntitySheet(@RequestParam("entitySheet") MultipartFile entitySheet) {
        ApiResponse apiResponse = masterEntityService.parseEntitySheetV2(entitySheet);



        return ResponseEntity.status(HttpStatus.CREATED).body(apiResponse);
    }

    /**
     * Test endpoint - supports CSV and XLSX using EntitySheetReaderFactory
     */
//    @PostMapping(value = "/upload/v2", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
//    public ResponseEntity<List<EntitySheetRow>> uploadEntitySheetV2(@RequestParam("file") MultipartFile file) {
//        List<EntitySheetRow> rows = masterEntityService.parseEntitySheetV2(file);
//        return ResponseEntity.ok(rows);
//    }

    /**
     * Create a new entity
     */
    @PostMapping
    public ResponseEntity<MasterEntity> create(@RequestBody MasterEntity entity) {
        MasterEntity created = masterEntityService.create(entity);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Get entity by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<MasterEntity> getById(@PathVariable Integer id) {
        return masterEntityService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get all entities
     */
    @GetMapping
    public ResponseEntity<List<MasterEntity>> getAll() {
        List<MasterEntity> entities = masterEntityService.findAll();
        return ResponseEntity.ok(entities);
    }

    /**
     * Get entities by type
     */
    @GetMapping("/type/{type}")
    public ResponseEntity<List<MasterEntity>> getByType(@PathVariable String type) {
        List<MasterEntity> entities = masterEntityService.findByType(type);
        return ResponseEntity.ok(entities);
    }

    /**
     * Get entities by status
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<List<MasterEntity>> getByStatus(@PathVariable String status) {
        List<MasterEntity> entities = masterEntityService.findByStatus(status);
        return ResponseEntity.ok(entities);
    }

    /**
     * Search entities by keyword
     */
    @GetMapping("/search")
    public ResponseEntity<List<MasterEntity>> search(@RequestParam String keyword) {
        List<MasterEntity> entities = masterEntityService.search(keyword);
        return ResponseEntity.ok(entities);
    }
}
