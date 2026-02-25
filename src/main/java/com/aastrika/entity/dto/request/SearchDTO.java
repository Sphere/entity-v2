package com.aastrika.entity.dto.request;

import com.aastrika.entity.enums.EntityType;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SearchDTO {

  private EntityType entityType;
  private String language;
  private String query;
  private boolean strict;
  private List<String> field;
}