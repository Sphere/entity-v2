package com.aastrika.entity.service;

import com.aastrika.entity.dto.request.EntityUpdateDTO;
import com.aastrika.entity.dto.response.ApiResponse;
import com.aastrika.entity.dto.response.EntityResponseDTO;
import com.aastrika.entity.model.MasterEntity;
import java.util.List;
import java.util.Optional;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

public interface MasterEntityService {

  ApiResponse processAndUploadSheet(MultipartFile sheetFile, String userId);

  MasterEntity create(MasterEntity entity);

  MasterEntity update(Integer id, MasterEntity entity);

  ApiResponse update(EntityUpdateDTO updateDTO, String userId);

  List<MasterEntity> findByCode(String code);

  List<MasterEntity> findAll();

  List<MasterEntity> findByEntityType(String type);

  void delete(Integer id);
}
