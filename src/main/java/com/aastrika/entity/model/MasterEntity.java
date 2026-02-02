package com.aastrika.entity.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
    name = "master_entities",
    uniqueConstraints = @UniqueConstraint(columnNames = {"code", "language_code"}))
public class MasterEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @Column(name = "entity_id", length = 50)
  private String entityId;

  @Column(length = 100)
  private String type;

  @Column(length = 500)
  private String name;

  @Column(length = 2000)
  private String description;

  @Column(length = 50)
  private String status;

  @Column(length = 50)
  private String code;

  @Column(length = 10, name = "language_code")
  private String languageCode;

  @Column(length = 200)
  private String level;

  @Column(name = "level_id")
  private String levelId;

  @CreationTimestamp
  @Column(name = "created_at", updatable = false)
  private Date createdAt;

  @CreationTimestamp
  @Column(name = "updated_at")
  private Date updatedAt;

  @Column(name = "reviewed_at")
  private Date reviewedAt;

  @Column(name = "created_by")
  private String createdBy;

  @Column(name = "updated_by")
  private String updatedBy;

  @Column(name = "reviewed_by")
  private String reviewedBy;

  @Column(length = 500)
  private String source;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "additional_properties", columnDefinition = "json")
  private Map<String, Object> additionalProperties;

  @OneToMany(mappedBy = "masterEntity", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<CompetencyLevel> competencyLevels = new ArrayList<>();
}
