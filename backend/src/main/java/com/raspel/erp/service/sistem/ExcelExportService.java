package com.raspel.erp.service.sistem;

import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Service
public class ExcelExportService {

    /** Çok sayfalı dışa aktarımda satır türü. */
    public enum SatirTuru {
        /** Sütun başlığı (mavi zemin, beyaz kalın yazı). */
        BASLIK,
        /** Bölüm başlığı (gri zemin, kalın yazı). */
        BOLUM,
        /** Veri satırı (sayısal hücreler numeric yazılır). */
        VERI,
        /** Boş ayırıcı satır. */
        BOS
    }

    /** Çok sayfalı dışa aktarımda tek satır. */
    public record SayfaSatiri(SatirTuru tur, List<Object> hucreler) {

        public static SayfaSatiri baslik(Object... hucreler) {
            return new SayfaSatiri(SatirTuru.BASLIK, Arrays.asList(hucreler));
        }

        public static SayfaSatiri bolum(String baslik) {
            return new SayfaSatiri(SatirTuru.BOLUM, List.of(baslik));
        }

        public static SayfaSatiri veri(Object... hucreler) {
            return new SayfaSatiri(SatirTuru.VERI, Arrays.asList(hucreler));
        }

        public static SayfaSatiri bos() {
            return new SayfaSatiri(SatirTuru.BOS, List.of());
        }
    }

    /** Çok sayfalı dışa aktarımda bir sayfa tanımı. */
    public record Sayfa(String ad, List<SayfaSatiri> satirlar) {
    }

    /**
     * Excel formula injection koruması: hücre =, +, -, @, \t, \r ile başlıyorsa
     * başına tek tırnak ekleyerek formül olarak yorumlanmasını engeller.
     */
    private String formulKorumasi(String value) {
        if (value == null) return "";
        String s = value;
        if (s.isEmpty()) return s;
        char ilk = s.charAt(0);
        if (ilk == '=' || ilk == '+' || ilk == '-' || ilk == '@' || ilk == '\t' || ilk == '\r') {
            return "'" + s;
        }
        return s;
    }

    public byte[] export(String sheetName, String[] columns, List<Map<String, Object>> rows) {
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet(sheetName);

            Font headerFont = wb.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            CellStyle headerStyle = wb.createCellStyle();
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.ROYAL_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < columns.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerStyle);
            }

            CellStyle dateStyle = wb.createCellStyle();
            dateStyle.setDataFormat(wb.getCreationHelper().createDataFormat().getFormat("dd.MM.yyyy HH:mm"));

            for (int r = 0; r < rows.size(); r++) {
                Row row = sheet.createRow(r + 1);
                Map<String, Object> data = rows.get(r);
                for (int c = 0; c < columns.length; c++) {
                    Cell cell = row.createCell(c);
                    Object val = data.get(columns[c]);
                    if (val == null) {
                        cell.setCellValue("");
                    } else if (val instanceof Number n) {
                        cell.setCellValue(n.doubleValue());
                    } else if (val instanceof java.util.Date d) {
                        cell.setCellValue(d);
                        cell.setCellStyle(dateStyle);
                    } else {
                        cell.setCellValue(formulKorumasi(val.toString()));
                    }
                }
            }

            for (int i = 0; i < columns.length; i++) {
                sheet.autoSizeColumn(i);
            }

            wb.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Excel dışa aktarma hatası", e);
        }
    }

    /**
     * Birden çok sayfa, bölüm başlığı ve boş ayırıcı satır içeren çalışma
     * kitabı üretir. Sayısal hücreler numeric yazılır: ondalıklı değerler
     * {@code #,##0.00}, tam sayılar {@code #,##0} biçimindedir.
     */
    public byte[] exportCokluSayfa(List<Sayfa> sayfalar) {
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            CellStyle baslikStili = baslikStili(wb);
            CellStyle bolumStili = bolumStili(wb);
            CellStyle paraStili = sayiStili(wb, "#,##0.00");
            CellStyle tamSayiStili = sayiStili(wb, "#,##0");
            CellStyle tarihStili = wb.createCellStyle();
            tarihStili.setDataFormat(wb.getCreationHelper().createDataFormat().getFormat("dd.MM.yyyy HH:mm"));

            for (Sayfa sayfa : sayfalar) {
                Sheet sheet = wb.createSheet(sayfa.ad());
                int satirNo = 0;
                int enGenisSutun = 0;
                for (SayfaSatiri satir : sayfa.satirlar()) {
                    Row row = sheet.createRow(satirNo++);
                    if (satir.tur() == SatirTuru.BOS) {
                        continue;
                    }
                    List<Object> hucreler = satir.hucreler();
                    enGenisSutun = Math.max(enGenisSutun, hucreler.size());
                    for (int c = 0; c < hucreler.size(); c++) {
                        Cell cell = row.createCell(c);
                        Object deger = hucreler.get(c);
                        switch (satir.tur()) {
                            case BASLIK -> {
                                cell.setCellValue(deger != null ? deger.toString() : "");
                                cell.setCellStyle(baslikStili);
                            }
                            case BOLUM -> {
                                cell.setCellValue(deger != null ? deger.toString() : "");
                                cell.setCellStyle(bolumStili);
                            }
                            case VERI -> yazVeri(cell, deger, paraStili, tamSayiStili, tarihStili);
                            default -> {
                                // BOS yukarıda ele alındı.
                            }
                        }
                    }
                }
                for (int i = 0; i < enGenisSutun; i++) {
                    sheet.autoSizeColumn(i);
                }
            }

            wb.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Excel dışa aktarma hatası", e);
        }
    }

    private CellStyle baslikStili(Workbook wb) {
        Font headerFont = wb.createFont();
        headerFont.setBold(true);
        headerFont.setColor(IndexedColors.WHITE.getIndex());
        CellStyle headerStyle = wb.createCellStyle();
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(IndexedColors.ROYAL_BLUE.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return headerStyle;
    }

    private CellStyle bolumStili(Workbook wb) {
        Font font = wb.createFont();
        font.setBold(true);
        CellStyle style = wb.createCellStyle();
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    private CellStyle sayiStili(Workbook wb, String format) {
        CellStyle style = wb.createCellStyle();
        style.setDataFormat(wb.getCreationHelper().createDataFormat().getFormat(format));
        return style;
    }

    private void yazVeri(Cell cell, Object deger, CellStyle paraStili, CellStyle tamSayiStili, CellStyle tarihStili) {
        if (deger == null) {
            cell.setCellValue("");
        } else if (deger instanceof BigDecimal bd) {
            cell.setCellValue(bd.doubleValue());
            cell.setCellStyle(paraStili);
        } else if (deger instanceof Integer || deger instanceof Long
                || deger instanceof Short || deger instanceof Byte) {
            cell.setCellValue(((Number) deger).doubleValue());
            cell.setCellStyle(tamSayiStili);
        } else if (deger instanceof Number n) {
            cell.setCellValue(n.doubleValue());
            cell.setCellStyle(paraStili);
        } else if (deger instanceof java.util.Date d) {
            cell.setCellValue(d);
            cell.setCellStyle(tarihStili);
        } else {
            cell.setCellValue(formulKorumasi(deger.toString()));
        }
    }
}
