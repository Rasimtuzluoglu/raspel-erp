package com.raspel.erp.service;

import com.raspel.erp.service.sistem.ExcelExportService;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ExcelExportServiceTest {

    private final ExcelExportService excelExportService = new ExcelExportService();

    @Test
    void export_returnsNonEmptyBytes() {
        byte[] bytes = excelExportService.export("Test", new String[]{"Ad", "Miktar"},
                List.of(Map.of("Ad", "Kalem", "Miktar", 10)));
        assertNotNull(bytes);
        assertTrue(bytes.length > 0);
    }

    @Test
    void export_writesHeaderAndData() throws Exception {
        byte[] bytes = excelExportService.export("Test", new String[]{"Ad", "Miktar"},
                List.of(Map.of("Ad", "Kalem", "Miktar", 10)));
        try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            Sheet sheet = wb.getSheet("Test");
            assertNotNull(sheet);
            Row header = sheet.getRow(0);
            assertEquals("Ad", header.getCell(0).getStringCellValue());
            assertEquals("Miktar", header.getCell(1).getStringCellValue());
            Row row = sheet.getRow(1);
            assertEquals("Kalem", row.getCell(0).getStringCellValue());
            assertEquals(10.0, row.getCell(1).getNumericCellValue());
        }
    }

    @Test
    void export_handlesNumberAndDateValues() throws Exception {
        Date tarih = new Date();
        byte[] bytes = excelExportService.export("Test", new String[]{"Tarih", "Tutar"},
                List.of(Map.of("Tarih", tarih, "Tutar", new BigDecimal("99.90"))));
        try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            Row row = wb.getSheet("Test").getRow(1);
            assertEquals(99.9, row.getCell(1).getNumericCellValue());
        }
    }

    @Test
    void export_emptyRows_stillWritesHeader() throws Exception {
        byte[] bytes = excelExportService.export("Test", new String[]{"Ad"}, List.of());
        try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            assertNotNull(wb.getSheet("Test").getRow(0));
            assertNull(wb.getSheet("Test").getRow(1));
        }
    }

    @Test
    void export_nullValues_areWrittenEmpty() throws Exception {
        Map<String, Object> satir = new HashMap<>();
        satir.put("Ad", "Kalem");
        satir.put("Aciklama", null);
        byte[] bytes = excelExportService.export("Test", new String[]{"Ad", "Aciklama"}, List.of(satir));
        try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            Row row = wb.getSheet("Test").getRow(1);
            assertEquals("", row.getCell(1).getStringCellValue());
        }
    }
}
