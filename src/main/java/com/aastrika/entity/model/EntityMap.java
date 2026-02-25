package com.aastrika.entity.model;

import com.aastrika.entity.enums.EntityType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "entity_map", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"parent_entity_code", "child_entity_code"})
})
public class EntityMap {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @Enumerated(EnumType.STRING)
  @Column(name = "parent_entity_type")
  private EntityType parentEntityType;

  @Column(name = "parent_entity_code")
  private String parentEntityCode;

  @Enumerated(EnumType.STRING)
  @Column(name = "child_entity_type")
  private EntityType childEntityType;

  @Column(name = "child_entity_code")
  private String childEntityCode;

  @Column(name = "competency_list")
  private String competencyLevelList;
}
