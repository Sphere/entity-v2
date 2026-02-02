package com.aastrika.entity.util;

import com.aastrika.entity.config.EntitySheetProperties;
import com.aastrika.entity.dto.EntitySheetRow;
import com.aastrika.entity.model.CompetencyLevel;
import com.aastrika.entity.model.MasterEntity;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import lombok.RequiredArgsConstructor;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.util.StringUtil;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SheetUtil {

  private final EntitySheetProperties entitySheetProperties;

  record CompetencyData(String name, String description) {}

  private List<CompetencyLevel> getCompetencyListByEntity(
      EntitySheetRow entitySheetRow, MasterEntity masterEntity) {
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

    return null;
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

  private static final Map<String, BiConsumer<EntitySheetRow, String>> FIELD_SETTERS =
      Map.of(
          "entity_id", EntitySheetRow::setEntityId,
          "name", EntitySheetRow::setName,
          "description", EntitySheetRow::setDescription,
          "code",
              new BiConsumer<EntitySheetRow, String>() {
                @Override
                public void accept(EntitySheetRow entitySheetRow, String value) {
                  entitySheetRow.setCode(value);
                }
              }
          // ... add all mappings
          );

  /**
   * @param csvRecords It should be non-null
   * @return
   */
  public List<EntitySheetRow> mapSheetToEntitySheetRow(@NonNull List<CSVRecord> csvRecords) {
    List<EntitySheetRow> entitySheetRows = new ArrayList<>();

    for (CSVRecord csvRecord : csvRecords) {
      EntitySheetRow entitySheetRow = new EntitySheetRow();
      BeanWrapper wrapper = new BeanWrapperImpl(entitySheetRow);

      for (Map.Entry<String, String> headerFieldEntry :
          entitySheetProperties.getHeaderFieldMappings().entrySet()) {
        String headerName = headerFieldEntry.getKey();
        String fieldName = headerFieldEntry.getValue();

        if (csvRecord.isMapped(headerName)) {
          wrapper.setPropertyValue(fieldName, csvRecord.get(headerName));
        }
      }
      entitySheetRows.add(entitySheetRow);
    }

    return entitySheetRows;
  }
}
