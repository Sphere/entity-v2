package com.aastrika.entity.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "competency_level")
public class CompetencyLevel {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumns({
    @JoinColumn(name = "code", referencedColumnName = "code", nullable = false),
    @JoinColumn(name = "language_code", referencedColumnName = "language_code", nullable = false)
  })
  private MasterEntity masterEntity;

  @Column(name = "level_number")
  private Integer levelNumber;

  @Column(name = "level_name")
  private String levelName;

  @Column(name = "level_description")
  private String levelDescription;
}
