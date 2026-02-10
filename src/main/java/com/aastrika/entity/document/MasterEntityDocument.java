package com.aastrika.entity.document;

import java.util.Date;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.WriteTypeHint;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "master_entities", writeTypeHint = WriteTypeHint.FALSE)
public class MasterEntityDocument {

  @Id private String id;

  @Field(type = FieldType.Keyword)
  private String entityId;

  @Field(type = FieldType.Text)
  private String entityType;

  @Field(type = FieldType.Keyword)
  private String type;

  @Field(type = FieldType.Text)
  private String code;

  @Field(type = FieldType.Text, analyzer = "standard")
  private String name;

  @Field(type = FieldType.Text)
  private String description;

  @Field(type = FieldType.Keyword)
  private String status;

  @Field(type = FieldType.Object)
  private Map<String, Object> additionalProperties;

  @Field(type = FieldType.Text)
  private String level;

  @Field(type = FieldType.Text)
  private String levelId;

  @Field(type = FieldType.Keyword)
  private String languageCode;

  @Field(type = FieldType.Date)
  private Date createdAt;

  @Field(type = FieldType.Date)
  private Date updatedAt;

  @Field(type = FieldType.Keyword)
  private String createdBy;

  @Field(type = FieldType.Keyword)
  private String updatedBy;

  // Competency Level 1
  @Field(type = FieldType.Text, analyzer = "standard")
  private String competencyLevel1Name;

  @Field(type = FieldType.Text)
  private String competencyLevel1Description;

  // Competency Level 2
  @Field(type = FieldType.Text, analyzer = "standard")
  private String competencyLevel2Name;

  @Field(type = FieldType.Text)
  private String competencyLevel2Description;

  // Competency Level 3
  @Field(type = FieldType.Text, analyzer = "standard")
  private String competencyLevel3Name;

  @Field(type = FieldType.Text)
  private String competencyLevel3Description;

  // Competency Level 4
  @Field(type = FieldType.Text, analyzer = "standard")
  private String competencyLevel4Name;

  @Field(type = FieldType.Text)
  private String competencyLevel4Description;

  // Competency Level 5
  @Field(type = FieldType.Text, analyzer = "standard")
  private String competencyLevel5Name;

  @Field(type = FieldType.Text)
  private String competencyLevel5Description;
}
