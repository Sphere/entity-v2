package com.aastrika.entity.reader;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import com.aastrika.entity.mapper.MasterEntityMapper;
import com.aastrika.entity.repository.jpa.MasterEntityRepository;
import com.aastrika.entity.support.TestApplicationProperties;
import com.aastrika.entity.util.SheetUtil;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

/** Verifies reader selection against the real supports() implementations. */
class EntitySheetReaderFactoryTest {

  private CsvEntitySheetReader csvReader;
  private XlsxEntitySheetReader xlsxReader;
  private EntitySheetReaderFactory factory;

  private static final String XLSX_CONTENT_TYPE =
      "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

  @BeforeEach
  void setUp() {
    csvReader = new CsvEntitySheetReader(mock(MasterEntityMapper.class),
        mock(MasterEntityRepository.class),
        TestApplicationProperties.entitySheetProperties(),
        new SheetUtil(TestApplicationProperties.entitySheetProperties()));
    xlsxReader = new XlsxEntitySheetReader();
    factory = new EntitySheetReaderFactory(List.of(csvReader, xlsxReader));
  }

  @Test
  @DisplayName("init - should log the registered readers without failing")
  void shouldReportRegisteredReadersOnInit() {
    factory.init();
  }

  @Test
  @DisplayName("getSheetReader - should select by content type")
  void shouldSelectReaderByContentType() {
    assertAll(
        () -> assertSame(csvReader, factory.getSheetReader(file("data.bin", "text/csv"))),
        () -> assertSame(xlsxReader, factory.getSheetReader(file("data.bin", XLSX_CONTENT_TYPE)))
    );
  }

  @Test
  @DisplayName("getSheetReader - should fall back to the filename extension")
  void shouldSelectReaderByFileExtension() {
    assertAll(
        () -> assertSame(csvReader,
            factory.getSheetReader(file("entities.csv", "application/octet-stream"))),
        () -> assertSame(xlsxReader,
            factory.getSheetReader(file("entities.xlsx", "application/octet-stream")))
    );
  }

  @Test
  @DisplayName("getSheetReader - should throw naming the content type and filename when nothing matches")
  void shouldThrowForUnsupportedFileType() {
    MultipartFile file = file("entities.pdf", "application/pdf");

    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
        () -> factory.getSheetReader(file));

    assertAll(
        () -> assertTrue(ex.getMessage().contains("application/pdf")),
        () -> assertTrue(ex.getMessage().contains("entities.pdf"))
    );
  }

  @Test
  @DisplayName("getSheetReader - should throw when both content type and filename are absent")
  void shouldThrowWhenContentTypeAndFilenameAreMissing() {
    MultipartFile file = new MockMultipartFile("file", null, null, new byte[0]);

    assertThrows(IllegalArgumentException.class, () -> factory.getSheetReader(file));
  }

  private static MultipartFile file(String fileName, String contentType) {
    return new MockMultipartFile("file", fileName, contentType, new byte[0]);
  }
}
