package com.aastrika.entity.repository.jpa;

import com.aastrika.entity.model.MasterEntity;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.aastrika.entity.util.CodeLanguageProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface MasterEntityRepository extends JpaRepository<MasterEntity, Integer> {

  List<MasterEntity> findByEntityType(String type);

  List<MasterEntity> findByCode(String code);

  Optional<MasterEntity> findByCodeAndLanguageCode(String code, String languageCode);

  List<MasterEntity> findByStatus(String status);

  List<MasterEntity> findByTypeAndStatus(String type, String status);

  List<MasterEntity> findByNameContaining(String name);

  @Query("SELECT m.code, m.languageCode FROM MasterEntity m WHERE CONCAT(m.code, ':', m.languageCode) IN :pairs")
  List<Object[]>  findRecordsByCodeLanguagePairs(@Param("pairs") List<String> pairs);

//  @Query("SELECT CONCAT(m.code, '\": \"', m.languageCode) FROM MasterEntity m WHERE CONCAT(m.code, ':', m.languageCode) IN :pairs")
//  List<String> findByCodeLanguagePairs(@Param("pairs") List<String> pairs);

  @Query("SELECT m.code as code, m.languageCode as languageCode FROM MasterEntity m WHERE CONCAT(m.code, ':', m.languageCode) IN :pairs")
  List<CodeLanguageProjection> findByCodeLanguagePairs(@Param("pairs") List<String> pairs);

  List<MasterEntity> findByCodeInAndLanguageCode(List<String> entityCodes, String languageCode);
}
