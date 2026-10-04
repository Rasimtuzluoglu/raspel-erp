package com.raspel.erp.controller;

import com.raspel.erp.dto.sistem.RaporDTO;
import com.raspel.erp.service.sistem.RaporService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.test.annotation.DirtiesContext;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.containsString;
import com.raspel.erp.controller.sistem.RaporController;

@WebMvcTest(RaporController.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@AutoConfigureMockMvc(addFilters = false)
@Import(TestSecurityMocks.class)
class RaporControllerTest {

    @Autowired
    private MockMvc mockMvc;

    
    @MockBean
    private RaporService raporService;

    @MockBean
    private com.raspel.erp.service.sistem.PdfRaporService pdfRaporService;

    @MockBean
    private com.raspel.erp.service.sistem.KarlilikService karlilikService;

    @MockBean
    private com.raspel.erp.service.ticaret.FaturaGecmisService faturaGecmisService;

    @MockBean
    private com.raspel.erp.service.sistem.ExcelExportService excelExportService;

    @MockBean
    private com.raspel.erp.service.sistem.EmailService emailService;

    @MockBean
    private com.raspel.erp.service.sistem.EmailPolitikaService emailPolitikaService;

    @MockBean
    private com.raspel.erp.service.sistem.Gorunum360Service gorunum360Service;

    @Test
    void shouldGetCariEkstre() throws Exception {
        var dto = RaporDTO.CariEkstreDTO.builder().cariAd("ABC Müşteri").donemBasBakiye(BigDecimal.ZERO).donemSonBakiye(BigDecimal.valueOf(5000)).build();
        when(raporService.cariEkstreGetir(anyLong(), any(LocalDate.class), any(LocalDate.class))).thenReturn(dto);

        mockMvc.perform(get("/api/raporlar/cari-ekstre")
                        .param("cariHesapId", "1")
                        .param("baslangic", "2024-01-01")
                        .param("bitis", "2024-12-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cariAd").value("ABC Müşteri"));
    }

    @Test
    void shouldGetGelirGider() throws Exception {
        var dto = RaporDTO.GelirGiderOzetDTO.builder().toplamGelir(BigDecimal.valueOf(50000)).toplamGider(BigDecimal.valueOf(30000)).netKarZarar(BigDecimal.valueOf(20000)).build();
        when(raporService.gelirGiderOzeti(any(LocalDate.class), any(LocalDate.class), any())).thenReturn(dto);

        mockMvc.perform(get("/api/raporlar/gelir-gider")
                        .param("baslangic", "2024-01-01")
                        .param("bitis", "2024-12-31")
                        .requestAttr("sirketId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.toplamGelir").value(50000));
    }

    @Test
    void shouldGetKdvRaporu() throws Exception {
        var dto = RaporDTO.KdvRaporDTO.builder().toplamKdvCikis(BigDecimal.valueOf(1000)).toplamKdvGiris(BigDecimal.valueOf(500)).kdvFarki(BigDecimal.valueOf(500)).build();
        when(raporService.kdvRaporu(any(LocalDate.class), any(LocalDate.class), any())).thenReturn(dto);

        mockMvc.perform(get("/api/raporlar/kdv")
                        .param("baslangic", "2024-01-01")
                        .param("bitis", "2024-12-31")
                        .requestAttr("sirketId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kdvFarki").value(500));
    }

    @Test
    void shouldGetYaslandirma() throws Exception {
        var satir = RaporDTO.YaslandirmaDTO.builder()
                .cariHesapId(3L).cariAd("Müşteri")
                .kovalar(new java.util.LinkedHashMap<>(java.util.Map.of(
                        "VADEDI_GELMEMIS", BigDecimal.valueOf(6000),
                        "GUN_0_30", BigDecimal.ZERO,
                        "GUN_31_60", BigDecimal.valueOf(4000),
                        "GUN_61_90", BigDecimal.ZERO,
                        "GUN_90_PLUS", BigDecimal.ZERO)))
                .toplam(BigDecimal.valueOf(10000))
                .gecikmisTutar(BigDecimal.valueOf(4000))
                .enFazlaGecikmeGun(45)
                .ortalamaGecikmeGun(45.0)
                .build();
        var rapor = RaporDTO.YaslandirmaRaporDTO.builder()
                .satirlar(List.of(satir))
                .referansTarih(java.time.LocalDate.of(2026, 3, 15))
                .ozet(RaporDTO.YaslandirmaOzetDTO.builder()
                        .kovalar(new java.util.LinkedHashMap<>(java.util.Map.of(
                                "VADEDI_GELMEMIS", BigDecimal.valueOf(6000),
                                "GUN_0_30", BigDecimal.ZERO,
                                "GUN_31_60", BigDecimal.valueOf(4000),
                                "GUN_61_90", BigDecimal.ZERO,
                                "GUN_90_PLUS", BigDecimal.ZERO)))
                        .toplam(BigDecimal.valueOf(10000))
                        .gecikmisTutar(BigDecimal.valueOf(4000))
                        .cariSayisi(1)
                        .kovaSirasi(RaporService.YASLANDIRMA_KOVALARI)
                        .build())
                .build();
        when(raporService.yaslandirmaRaporu(any(), any())).thenReturn(rapor);

        mockMvc.perform(get("/api/raporlar/yaslandirma").requestAttr("sirketId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.satirlar[0].cariAd").value("Müşteri"))
                .andExpect(jsonPath("$.satirlar[0].kovalar.GUN_31_60").value(4000))
                .andExpect(jsonPath("$.satirlar[0].gecikmisTutar").value(4000))
                .andExpect(jsonPath("$.ozet.cariSayisi").value(1))
                .andExpect(jsonPath("$.referansTarih").value("2026-03-15"));
    }

    @Test
    void shouldGetYaslandirmaPdf() throws Exception {
        var rapor = RaporDTO.YaslandirmaRaporDTO.builder()
                .satirlar(List.of(RaporDTO.YaslandirmaDTO.builder()
                        .cariHesapId(3L).cariAd("Müşteri")
                        .kovalar(new java.util.LinkedHashMap<>(java.util.Map.of(
                                "VADEDI_GELMEMIS", BigDecimal.valueOf(6000),
                                "GUN_0_30", BigDecimal.ZERO,
                                "GUN_31_60", BigDecimal.valueOf(4000),
                                "GUN_61_90", BigDecimal.ZERO,
                                "GUN_90_PLUS", BigDecimal.ZERO)))
                        .toplam(BigDecimal.valueOf(10000))
                        .gecikmisTutar(BigDecimal.valueOf(4000))
                        .enFazlaGecikmeGun(45)
                        .build()))
                .referansTarih(java.time.LocalDate.of(2026, 3, 15))
                .ozet(RaporDTO.YaslandirmaOzetDTO.builder().build())
                .build();
        when(raporService.yaslandirmaRaporu(any(), any())).thenReturn(rapor);
        when(pdfRaporService.tabloRaporu(anyString(), any(), any())).thenReturn(new byte[]{1, 2, 3});

        mockMvc.perform(get("/api/raporlar/yaslandirma/pdf").requestAttr("sirketId", 1L))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("yaslandirma-2026-03-15.pdf")));
    }

    @Test
    void shouldGetNakitAkisiProjeksiyonu() throws Exception {
        var dto = com.raspel.erp.dto.sistem.NakitAkisiProjeksiyonDTO.builder()
                .baslangicBakiyesi(BigDecimal.valueOf(50000))
                .toplamBeklenenGiris(BigDecimal.valueOf(10000))
                .toplamBeklenenCikis(BigDecimal.valueOf(5000))
                .tahminiBitisBakiyesi(BigDecimal.valueOf(55000))
                .projeksiyonGunu(30)
                .gunlukAkis(List.of())
                .build();
        when(raporService.nakitAkisiProjeksiyonu(eq(30), any())).thenReturn(dto);

        mockMvc.perform(get("/api/raporlar/nakit-akisi-projeksiyonu")
                        .param("gun", "30")
                        .requestAttr("sirketId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.baslangicBakiyesi").value(50000))
                .andExpect(jsonPath("$.tahminiBitisBakiyesi").value(55000));
    }

    @Test
    void shouldGetCariEkstrePdf() throws Exception {
        var dto = RaporDTO.CariEkstreDTO.builder().cariAd("ABC Müşteri").hareketler(List.of()).build();
        when(raporService.cariEkstreGetir(anyLong(), any(LocalDate.class), any(LocalDate.class))).thenReturn(dto);
        when(pdfRaporService.tabloRaporu(anyString(), any(), any())).thenReturn(new byte[]{1, 2, 3});

        mockMvc.perform(get("/api/raporlar/cari-ekstre/pdf")
                        .param("cariHesapId", "1")
                        .param("baslangic", "2024-01-01")
                        .param("bitis", "2024-12-31"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"));
    }

    @Test
    void shouldGetGelirGiderPdf() throws Exception {
        var dto = RaporDTO.GelirGiderOzetDTO.builder()
                .toplamGelir(BigDecimal.TEN).toplamGider(BigDecimal.ONE)
                .netKarZarar(BigDecimal.valueOf(9)).aylikDagilim(List.of()).build();
        when(raporService.gelirGiderOzeti(any(LocalDate.class), any(LocalDate.class), any())).thenReturn(dto);
        when(pdfRaporService.tabloRaporu(anyString(), any(), any())).thenReturn(new byte[]{1});

        mockMvc.perform(get("/api/raporlar/gelir-gider/pdf")
                        .param("baslangic", "2024-01-01")
                        .param("bitis", "2024-12-31")
                        .requestAttr("sirketId", 1L))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"));
    }

    @Test
    void shouldGetCariKarlilikPdf() throws Exception {
        var dto = RaporDTO.CariKarlilikDTO.builder()
                .toplamSatis(BigDecimal.TEN).toplamMaliyet(BigDecimal.ONE)
                .toplamKar(BigDecimal.valueOf(9)).satirlar(List.of()).build();
        when(raporService.cariKarlilikRaporu(any(LocalDate.class), any(LocalDate.class), any())).thenReturn(dto);
        when(pdfRaporService.tabloRaporu(anyString(), any(), any())).thenReturn(new byte[]{1});

        mockMvc.perform(get("/api/raporlar/cari-karlilik/pdf")
                        .param("baslangic", "2024-01-01")
                        .param("bitis", "2024-12-31")
                        .requestAttr("sirketId", 1L))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"));
    }
}





