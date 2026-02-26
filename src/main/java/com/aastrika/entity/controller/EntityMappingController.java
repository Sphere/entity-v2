package com.aastrika.entity.controller;

import com.aastrika.entity.dto.request.EntityMappingRequestDTO;
import com.aastrika.entity.dto.request.EntitySearchRequestDTO;
import com.aastrika.entity.dto.response.AppResponse;
import com.aastrika.entity.dto.response.EntityMappingResponseDTO;
import com.aastrika.entity.dto.response.FullHierarchyNodeDTO;
import com.aastrika.entity.dto.response.HierarchyResponseDTO;
import com.aastrika.entity.service.impl.EntityMappingServiceImpl;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/entity")
@RequiredArgsConstructor
public class EntityMappingController {

  private final EntityMappingServiceImpl entityMappingService;

  @PostMapping("/mapping")
  public ResponseEntity<AppResponse<List<EntityMappingResponseDTO>>> saveEntity(
      @Valid @RequestBody List<EntityMappingRequestDTO> entityMappingRequestDTOList) {
    List<EntityMappingResponseDTO> result = entityMappingService.saveEntityMapping(entityMappingRequestDTOList);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(AppResponse.success("api.entity.mapping", result, HttpStatus.CREATED));
  }

  @PostMapping("/mapping/search")
  public ResponseEntity<AppResponse<List<HierarchyResponseDTO>>> getEntityMappingDetails(
      @Valid @RequestBody EntitySearchRequestDTO entitySearchRequestDTO) {
    List<HierarchyResponseDTO> result = entityMappingService.getEntityMappingHierarchy(entitySearchRequestDTO);
    return ResponseEntity.ok(AppResponse.success("api.entity.mapping.search", result, HttpStatus.OK));
  }

  @PostMapping("/hierarchy")
  public ResponseEntity<AppResponse<FullHierarchyNodeDTO>> getFullHierarchy(
      @Valid @RequestBody EntitySearchRequestDTO entitySearchRequestDTO) {
    FullHierarchyNodeDTO result = entityMappingService.getFullHierarchy(entitySearchRequestDTO);
    return ResponseEntity.ok(AppResponse.success("api.entity.mapping.full-hierarchy", result, HttpStatus.OK));
  }

}
