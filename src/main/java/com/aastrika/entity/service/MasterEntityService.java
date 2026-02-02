package com.aastrika.entity.service;

import com.aastrika.entity.dto.EntitySheetRow;
import com.aastrika.entity.dto.response.ApiResponse;
import com.aastrika.entity.model.MasterEntity;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

public interface MasterEntityService {

    List<MasterEntity> parseEntitySheet(MultipartFile entitySheet);

    ApiResponse parseEntitySheetV2(MultipartFile file);

    MasterEntity create(MasterEntity entity);

    MasterEntity update(Integer id, MasterEntity entity);

    Optional<MasterEntity> findById(Integer id);

    List<MasterEntity> findAll();

    List<MasterEntity> findByType(String type);

    List<MasterEntity> findByStatus(String status);

    List<MasterEntity> findByTypeAndStatus(String type, String status);

    List<MasterEntity> search(String keyword);

    void delete(Integer id);
}
