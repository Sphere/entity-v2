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

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "master_entities")
public class MasterEntityDocument {

  @Id private Integer id;

  @Field(type = FieldType.Text)
  private String entityId;

  @Field(type = FieldType.Keyword)
  private String type;

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
}
