package com.aastrika.entity.reader;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class EntitySheetReaderFactory {

    private final List<EntitySheetReader> readers;

    @PostConstruct
    public void init() {
        log.info("EntitySheetReaderFactory initialized with {} readers:", readers.size());
        readers.forEach(reader -> log.info("  - {}", reader.getClass().getSimpleName()));
    }

    public EntitySheetReader getSheetReader(MultipartFile file) {
        String contentType = file.getContentType();
        String fileName = file.getOriginalFilename();

        log.info("Finding reader for contentType: {}, fileName: {}", contentType, fileName);

        return readers.stream()
                .filter(reader -> reader.supports(contentType, fileName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unsupported file type: " + contentType + ", fileName: " + fileName));
    }
}
