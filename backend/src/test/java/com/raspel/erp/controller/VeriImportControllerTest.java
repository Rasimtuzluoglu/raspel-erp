package com.raspel.erp.controller;

import com.raspel.erp.service.envanter.StokService;
import com.raspel.erp.service.finans.CariHesapService;
import com.raspel.erp.service.ticaret.FaturaService;
import com.raspel.erp.repository.envanter.StokRepository;
import com.raspel.erp.repository.finans.CariHesapRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.raspel.erp.entity.envanter.Stok;
import com.raspel.erp.dto.envanter.StokDTO;
import com.raspel.erp.controller.sistem.VeriImportController;

import static org.junit.jupiter.api.Assertions.assertEquals;

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

    @MockBean
    private FaturaService faturaService;

    @MockBean
    private StokRepository stokRepository;

    @MockBean
    private CariHesapRepository cariHesapRepository;

    @MockBean
    private com.raspel.erp.service.finans.HareketService hareketService;

    // Metot duzeyindeki @PreAuthorize ifadeleri bu beani SpEL ile cagirir.
    // Bu testte filtreler kapali (addFilters = false), dolayisiyla cagri
    // yapilmaz; ancak bean bulunmazsa context ayaga kalkamaz.
    @MockBean
    private com.raspel.erp.config.security.YetkiKontrol yetkiKontrol;

    @Test
    void shouldImportStokFromCsv() throws Exception {
        String csv = "ad;stokKodu;barkod;birim;fiyat;miktar;minMiktar\n" +
                     "MDF 18mm;MDF-18;;Adet;850;100;10\n" +
                     "Sunta 16mm;SUNTA-16;;Adet;520;80;10";
        MockMultipartFile dosya = new MockMultipartFile("file", "stoklar.csv", "text/csv", csv.getBytes());
        when(stokService.topluOlustur(anyList(), eq(1L))).thenReturn(2);

        mockMvc.perform(multipart("/api/import/stok").file(dosya).requestAttr("sirketId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.basarili").value(2))
                .andExpect(jsonPath("$.hatalar").isEmpty());

        verify(stokService, times(1)).topluOlustur(argThat(list -> ((List<?>) list).size() == 2), eq(1L));
    }

    @Test
    void shouldReportErrorsForInvalidRows() throws Exception {
        String csv = "ad;stokKodu;fiyat\n" +
                     "Geçerli Ürün;KOD1;100\n" +
                     ";;50\n"; // ad eksik -> hata
        MockMultipartFile dosya = new MockMultipartFile("file", "stoklar.csv", "text/csv", csv.getBytes());
        when(stokService.topluOlustur(anyList(), eq(1L))).thenReturn(1);

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
    void shouldImportHareketFromCsv() throws Exception {
        String csv = "cariId;tarih;tur;tutar;aciklama\n" +
                     "5;2026-01-10;TAHSILAT;1500;Nakit tahsilat\n" +
                     "5;2026-01-11;ODEME;500;Ödeme";
        MockMultipartFile dosya = new MockMultipartFile("file", "hareketler.csv", "text/csv", csv.getBytes());
        when(hareketService.hareketOlustur(any(), eq(1L))).thenReturn(null);

        mockMvc.perform(multipart("/api/import/hareket").file(dosya).requestAttr("sirketId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.basarili").value(2))
                .andExpect(jsonPath("$.hatalar").isEmpty());

        verify(hareketService, times(2)).hareketOlustur(any(), eq(1L));
    }

    @Test
    void shouldReturn400ForEmptyFile() throws Exception {
        MockMultipartFile bos = new MockMultipartFile("file", "bos.csv", "text/csv", new byte[0]);

        mockMvc.perform(multipart("/api/import/stok").file(bos).requestAttr("sirketId", 1L))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldParseRafNoColumn() throws Exception {
        // TopluStok ekrani kaldirildi; rafNo destegi import ucuna tasindi.
        String csv = "ad;stokKodu;rafNo\n" +
                     "Urun A;A-1;B3\n";
        MockMultipartFile dosya = new MockMultipartFile("file", "stoklar.csv", "text/csv", csv.getBytes());
        when(stokService.topluOlustur(anyList(), eq(1L))).thenReturn(1);

        mockMvc.perform(multipart("/api/import/stok").file(dosya).requestAttr("sirketId", 1L))
                .andExpect(status().isOk());

        ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
        verify(stokService, times(1)).topluOlustur(captor.capture(), eq(1L));
        StokDTO dto = (StokDTO) captor.getValue().get(0);
        assertEquals("B3", dto.getRafNo());
    }
}