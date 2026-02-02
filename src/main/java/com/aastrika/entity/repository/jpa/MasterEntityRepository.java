package com.aastrika.entity.repository.jpa;

import com.aastrika.entity.model.MasterEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MasterEntityRepository extends JpaRepository<MasterEntity, Integer> {

  List<MasterEntity> findByType(String type);

  List<MasterEntity> findByStatus(String status);

  List<MasterEntity> findByTypeAndStatus(String type, String status);

  List<MasterEntity> findByNameContaining(String name);
}
