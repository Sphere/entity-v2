package com.aastrika.entity.reader;

import com.aastrika.entity.dto.EntitySheetRow;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface EntitySheetReader {

    List<EntitySheetRow> read(MultipartFile file);

    boolean supports(String contentType, String fileName);
}
