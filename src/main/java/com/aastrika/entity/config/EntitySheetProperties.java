package com.aastrika.entity.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "entity-sheet")
public class EntitySheetProperties {

  private Headers headers = new Headers();
  private Map<String, String> headerFieldMappings = new HashMap<>();
  private Integer competencyLevelSize = 5;

  @Data
  public static class Headers {
    private List<String> required = new ArrayList<>();
    private List<String> optional = new ArrayList<>();
    private List<String> competencyLevels = new ArrayList<>();

    public List<String> getAllHeaders() {
      List<String> all = new ArrayList<>();
      all.addAll(required);
      all.addAll(optional);
      all.addAll(competencyLevels);
      return all;
    }
  }
}
