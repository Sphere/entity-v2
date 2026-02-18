package com.aastrika.entity.repository.jpa;

import com.aastrika.entity.model.EntityMap;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EntityMapRepository extends JpaRepository<EntityMap, Integer> {

  Optional<EntityMap> findByParentEntityCodeAndChildEntityCode(String parentEntityCode, String childEntityCode);

  List<EntityMap> findByParentEntityCodeAndParentEntityType(String parentEntityCode, String parentEntityType);

}
