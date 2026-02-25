package com.aastrika.entity.service;

import com.aastrika.entity.dto.request.EntityCreateRequestDTO;
import com.aastrika.entity.dto.request.EntityDeleteRequestDTO;
import com.aastrika.entity.dto.request.EntityUpdateDTO;
import com.aastrika.entity.dto.response.AppResponse;
import com.aastrika.entity.model.MasterEntity;
import java.util.List;

import org.springframework.web.multipart.MultipartFile;

public interface MasterEntityService {

  AppResponse processAndUploadSheet(MultipartFile sheetFile, String userId);

  AppResponse create(EntityCreateRequestDTO entityCreateRequestDTO, String userId);

  AppResponse update(EntityUpdateDTO updateDTO, String userId);

  AppResponse deleteMasterEntities(List<EntityDeleteRequestDTO> deleteRequests);
}
