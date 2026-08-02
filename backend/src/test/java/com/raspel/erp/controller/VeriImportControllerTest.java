package com.raspel.erp.controller;

import com.raspel.erp.service.StokService;
import com.raspel.erp.service.CariHesapService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(VeriImportController.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@AutoConfigureMockMvc(addFilters = false)
@Import(TestSecurityMocks.class)
class VeriImportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StokService stokService;

    @MockBean
    private CariHesapService cariHesapService;

    @Test
    void shouldImportStokFromCsv() throws Exception {
        String csv = "ad;stokKodu;barkod;birim;fiyat;miktar;minMiktar\n" +
                     "MDF 18mm;MDF-18;;Adet;850;100;10\n" +
                     "Sunta 16mm;SUNTA-16;;Adet;520;80;10";
        MockMultipartFile dosya = new MockMultipartFile("file", "stoklar.csv", "text/csv", csv.getBytes());

        mockMvc.perform(multipart("/api/import/stok").file(dosya).requestAttr("sirketId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.basarili").value(2))
                .andExpect(jsonPath("$.hatalar").isEmpty());

        verify(stokService, times(2)).olustur(any(), eq(1L));
    }

    @Test
    void shouldReportErrorsForInvalidRows() throws Exception {
        String csv = "ad;stokKodu;fiyat\n" +
                     "Geçerli Ürün;KOD1;100\n" +
                     ";;50\n"; // ad eksik -> hata
        MockMultipartFile dosya = new MockMultipartFile("file", "stoklar.csv", "text/csv", csv.getBytes());

        mockMvc.perform(multipart("/api/import/stok").file(dosya).requestAttr("sirketId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.basarili").value(1))
                .andExpect(jsonPath("$.hatalar.length()").value(1));
    }

    @Test
    void shouldImportCariFromCsv() throws Exception {
        String csv = "ad;vergiNo;telefon;eposta;il;ilce;adres\n" +
                     "Demo Müşteri;1111111111;05321111111;demo@test.com;İstanbul;Kadıköy;Test Cad.";
        MockMultipartFile dosya = new MockMultipartFile("file", "cariler.csv", "text/csv", csv.getBytes());

        mockMvc.perform(multipart("/api/import/cari").file(dosya).requestAttr("sirketId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.basarili").value(1));

        verify(cariHesapService, times(1)).cariHesapOlustur(any(), eq(1L));
    }

    @Test
    void shouldReturn400ForEmptyFile() throws Exception {
        MockMultipartFile bos = new MockMultipartFile("file", "bos.csv", "text/csv", new byte[0]);

        mockMvc.perform(multipart("/api/import/stok").file(bos).requestAttr("sirketId", 1L))
                .andExpect(status().isBadRequest());
    }
}
