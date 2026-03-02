package com.aastrika.entity.repository.jpa;

import com.aastrika.entity.enums.EntityType;
import com.aastrika.entity.model.EntityMap;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EntityMapRepository extends JpaRepository<EntityMap, Integer> {

  Optional<EntityMap> findByParentEntityCodeAndChildEntityCode(String parentEntityCode, String childEntityCode);

  List<EntityMap> findByParentEntityCodeAndParentEntityType(String parentEntityCode, EntityType parentEntityType);

  @Modifying
  @Query("DELETE FROM EntityMap e WHERE e.parentEntityCode = :code AND e.parentEntityType = :entityType")
  void deleteByParentEntityCodeAndParentEntityType(@Param("code") String code,
                                                   @Param("entityType") EntityType entityType);

  @Modifying
  @Query("DELETE FROM EntityMap e WHERE e.childEntityCode = :code AND e.childEntityType = :entityType")
  void deleteByChildEntityCodeAndChildEntityType(@Param("code") String code,
                                                 @Param("entityType") EntityType entityType);

  boolean existsByParentEntityCodeAndParentEntityType(String parentEntityCode, EntityType parentEntityType);

  boolean existsByChildEntityCodeAndChildEntityType(String childEntityCode, EntityType childEntityType);

  List<EntityMap> findByParentEntityCodeIn(List<String> parentEntityCodes);
}
