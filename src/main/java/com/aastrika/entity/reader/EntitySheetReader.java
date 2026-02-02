package com.aastrika.entity.reader;

import com.aastrika.entity.dto.EntitySheetRow;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

public interface EntitySheetReader {

  List<EntitySheetRow> read(MultipartFile file);

  boolean supports(String contentType, String fileName);
}
