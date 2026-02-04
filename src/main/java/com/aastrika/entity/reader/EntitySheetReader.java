package com.aastrika.entity.reader;

import com.aastrika.entity.dto.EntitySheetRow;
import java.util.List;
import java.util.Map;

import org.springframework.web.multipart.MultipartFile;

public interface EntitySheetReader {

  public String getGlobalEntityType();

  public Map<String, List<EntitySheetRow>> getCompiledEntitySheet(MultipartFile entitySheet);

  boolean supports(String contentType, String fileName);
}
