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
   * Maps sheet to EntitySheetRow.
   * Uses {@link EntitySheetRow#rowNumber} for tracking.
   *
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
      /* "rowNumber" string literal is an exceptional case. It refers to
       * {@link EntitySheetRow#rowNumber} field. It is tightly coupled with EntitySheetRow. */
      wrapper.setPropertyValue("rowNumber", csvRecord.getRecordNumber() + 1);
      entitySheetRows.add(entitySheetRow);
    }

    return entitySheetRows;
  }
}
