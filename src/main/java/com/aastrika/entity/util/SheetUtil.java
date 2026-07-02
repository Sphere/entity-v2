package com.aastrika.entity.util;

import com.aastrika.entity.config.EntitySheetProperties;
import com.aastrika.entity.dto.EntitySheetRow;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.apache.commons.csv.CSVRecord;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SheetUtil {

  private final EntitySheetProperties entitySheetProperties;

  /**
   * Converts each CSV record into an {@link EntitySheetRow} by reading the header-to-field
   * mappings from {@link EntitySheetProperties#getHeaderFieldMappings()}.
   * The map key is the CSV column header name; the value is the corresponding
   * {@link EntitySheetRow} field name used for reflection-based assignment via {@link BeanWrapper}.
   */
  public List<EntitySheetRow> mapSheetToEntitySheetRow(@NonNull List<CSVRecord> csvRecords) {
    List<EntitySheetRow> entitySheetRows = new ArrayList<>();

    for (CSVRecord csvRecord : csvRecords) {
      EntitySheetRow entitySheetRow = new EntitySheetRow();
      BeanWrapper wrapper = new BeanWrapperImpl(entitySheetRow);

      for (Map.Entry<String, String> headerFieldEntry :
          entitySheetProperties.getHeaderFieldMappings().entrySet()) {

        String sheetColumnHeader = headerFieldEntry.getKey();
        String entitySheetRowField = headerFieldEntry.getValue();

        if (csvRecord.isMapped(sheetColumnHeader)) {
          wrapper.setPropertyValue(entitySheetRowField, csvRecord.get(sheetColumnHeader));
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
