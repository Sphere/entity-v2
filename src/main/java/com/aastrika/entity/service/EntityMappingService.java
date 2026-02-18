package com.aastrika.entity.service;

import com.aastrika.entity.dto.request.EntityMappingRequestDTO;
import com.aastrika.entity.dto.request.EntitySearchRequestDTO;
import com.aastrika.entity.dto.response.EntityMappingResponseDTO;
import com.aastrika.entity.dto.response.HierarchyResponseDTO;

import java.util.List;

public interface EntityMappingService {
  List<EntityMappingResponseDTO> saveEntityMapping(List<EntityMappingRequestDTO> entityMappingRequestDTOList);

  List<HierarchyResponseDTO> getEntityMappingHierarchy(EntitySearchRequestDTO entitySearchRequestDTO);
}
