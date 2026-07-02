package com.aastrika.entity.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

  @Column(name = "parent_entity_type")
  private String parentEntityType;

  @Column(name = "parent_entity_code")
  private String parentEntityCode;

  @Column(name = "child_entity_type")
  private String childEntityType;

  @Column(name = "child_entity_code")
  private String childEntityCode;

  @Column(name = "competency_list")
  private String competencyLevelList;
}
