package com.aastrika.entity.util;

import com.aastrika.entity.config.EntitySheetProperties;
import com.aastrika.entity.dto.EntitySheetRow;
import com.aastrika.entity.model.CompetencyLevel;
import com.aastrika.entity.model.MasterEntity;
import lombok.RequiredArgsConstructor;
import org.apache.poi.util.StringUtil;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class EntityUtil {
  private final EntitySheetProperties entitySheetProperties;

  record CompetencyData(String name, String description) {}

  /**
   * @param entitySheetRow
   * @return
   */
  public List<CompetencyLevel> getCompetencyListByEntity(EntitySheetRow entitySheetRow, MasterEntity masterEntity) {

    List<CompetencyLevel> competencyLevelList = new ArrayList<>();

    for (int levelIndex = 1;
         levelIndex <= entitySheetProperties.getCompetencyLevelSize();
         levelIndex++) {
      CompetencyData competencyData = getCompetencyData(entitySheetRow, levelIndex);

      if (competencyData != null
        && !StringUtil.isBlank(competencyData.name)
        && !StringUtil.isBlank(competencyData.description)) {

        CompetencyLevel competencyLevel =
          CompetencyLevel.builder()
            .levelNumber(levelIndex)
            .levelName(competencyData.name)
            .levelDescription(competencyData.description)
            .masterEntity(masterEntity)
            .build();

        competencyLevelList.add(competencyLevel);
      }
    }
    return competencyLevelList;
  }

  private CompetencyData getCompetencyData(EntitySheetRow entitySheetRow, int levelIndex) {
    return switch (levelIndex) {
      case 1 -> new CompetencyData(
        entitySheetRow.getCompetencyLevel1Name(),
        entitySheetRow.getCompetencyLevel1Description());
      case 2 -> new CompetencyData(
        entitySheetRow.getCompetencyLevel2Name(),
        entitySheetRow.getCompetencyLevel2Description());
      case 3 -> new CompetencyData(
        entitySheetRow.getCompetencyLevel3Name(),
        entitySheetRow.getCompetencyLevel3Description());
      case 4 -> new CompetencyData(
        entitySheetRow.getCompetencyLevel4Name(),
        entitySheetRow.getCompetencyLevel4Description());
      case 5 -> new CompetencyData(
        entitySheetRow.getCompetencyLevel5Name(),
        entitySheetRow.getCompetencyLevel5Description());
      default -> null;
    };
  }

  public Map<String, List<Integer>> getMissingDataDetails(List<EntitySheetRow> entitySheetRowList) {
    Map<String, List<Integer>> missedDataMap = new HashMap<>();

    if (entitySheetRowList != null && !entitySheetRowList.isEmpty()) {
      for (EntitySheetRow entitySheetRow : entitySheetRowList) {
        if (StringUtil.isBlank(entitySheetRow.getEntityId())) {
          List<Integer> rowNumbers = missedDataMap.get(entitySheetRow.getEntityId());

        }
      }

    }
    return missedDataMap;
  }

  public void buildMissedDataMap(Map<String, List<Integer>> missedDataMap, String fieldName,
                                 EntitySheetRow entitySheetRow) {
    BeanWrapper wrapper = new BeanWrapperImpl(entitySheetRow);
    wrapper.getPropertyValue(fieldName);
  }
}
