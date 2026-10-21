package com.raspel.erp.controller.sistem;

import com.raspel.erp.controller.TestSecurityMocks;
import com.raspel.erp.dto.finans.CariHesapDTO;
import com.raspel.erp.service.sistem.AuditLogService;
import com.raspel.erp.service.sistem.ExcelExportService;
import com.raspel.erp.service.finans.BankaService;
import com.raspel.erp.service.finans.CariHesapService;
import com.raspel.erp.service.finans.HareketService;
import com.raspel.erp.service.finans.KasaService;
import com.raspel.erp.service.ik.PersonelService;
import com.raspel.erp.service.envanter.StokService;
import com.raspel.erp.service.muhasebe.MuhasebeService;
import com.raspel.erp.service.ticaret.FaturaService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ExcelExportController.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@AutoConfigureMockMvc(addFilters = false)
@Import(TestSecurityMocks.class)
class ExcelExportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ExcelExportController controller;

    @MockBean
    private ExcelExportService excelService;
    @MockBean
    private CariHesapService cariHesapService;
    @MockBean
    private FaturaService faturaService;
    @MockBean
    private HareketService hareketService;
    @MockBean
    private StokService stokService;
    @MockBean
    private PersonelService personelService;
    @MockBean
    private BankaService bankaService;
    @MockBean
    private KasaService kasaService;
    @MockBean
    private AuditLogService auditLogService;
    @MockBean
    private MuhasebeService muhasebeService;
    @MockBean
    private com.raspel.erp.service.sistem.RaporService raporService;
    @MockBean
    private com.raspel.erp.service.ticaret.SiparisTakipService siparisTakipService;
    @MockBean
    private com.raspel.erp.service.ticaret.TeslimatService teslimatService;
    @MockBean
    private com.raspel.erp.service.envanter.UretimService uretimService;
    @MockBean
    private com.raspel.erp.service.sistem.Gorunum360Service gorunum360Service;
    @MockBean
    private com.raspel.erp.service.sistem.DonemService donemService;
    @MockBean
    private com.raspel.erp.service.envanter.StokSayimService stokSayimService;
    @MockBean
    private com.raspel.erp.service.envanter.StokSeriService stokSeriService;
    @MockBean
    private com.raspel.erp.service.sistem.AnomaliTespitEngine anomaliTespitEngine;
    @MockBean
    private com.raspel.erp.service.sistem.IpWhitelistService ipWhitelistService;
    @MockBean
    private com.raspel.erp.service.sube.SubeService subeService;

    @Test
    void cariHesaplar_excelDoner() throws Exception {
        CariHesapDTO c = CariHesapDTO.builder().id(1L).ad("Müşteri").vergiNumarasi("123").build();
        when(cariHesapService.tumCariHesaplariGetir(any(), any())).thenReturn(new PageImpl<>(List.of(c)));
        when(excelService.export(any(), any(), any())).thenReturn(new byte[]{1, 2, 3});

        mockMvc.perform(get("/api/exports/cari-hesaplar"))
                .andExpect(status().isOk())
                .andExpect(content().bytes(new byte[]{1, 2, 3}));
    }

    @Test
    void excel_limitAsilincaUyariSatiriVeHeaderEkler() {
        java.util.List<java.util.Map<String, Object>> rows = new java.util.ArrayList<>();
        for (int i = 0; i < 10000; i++) rows.add(java.util.Map.of("A", "x"));
        when(excelService.export(any(), any(), any())).thenReturn(new byte[]{9});

        var resp = (org.springframework.http.ResponseEntity<byte[]>) org.springframework.test.util.ReflectionTestUtils
                .invokeMethod(controller, "excel", "Test", new String[]{"A"}, rows);

        org.junit.jupiter.api.Assertions.assertNotNull(resp);
        org.junit.jupiter.api.Assertions.assertEquals("true", resp.getHeaders().getFirst("X-Export-Truncated"));
        // Servise gonderilen listede +1 gorunur uyari satiri olmali.
        org.mockito.Mockito.verify(excelService).export(
                org.mockito.ArgumentMatchers.eq("Test"),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.argThat(l -> l != null && l.size() == 10001));
    }

    @Test
    void guvenliDosyaAdi_baslikBozanKarakterleriTemizler() {
        String sonuc = org.springframework.test.util.ReflectionTestUtils
                .invokeMethod(controller, "guvenliDosyaAdi", "BA-BS\"\r\n;../x");

        org.junit.jupiter.api.Assertions.assertNotNull(sonuc);
        org.junit.jupiter.api.Assertions.assertFalse(sonuc.contains("\""));
        org.junit.jupiter.api.Assertions.assertFalse(sonuc.contains("\n"));
        org.junit.jupiter.api.Assertions.assertFalse(sonuc.contains(";"));
        org.junit.jupiter.api.Assertions.assertFalse(sonuc.contains("/"));
    }

    @Test
    void siparisTakip_excelDoner() throws Exception {
        when(siparisTakipService.filtreliZincir(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(com.raspel.erp.dto.ticaret.SiparisTakipDTO.builder()
                        .siparisId(1L).siparisNo("SP-1").siparisDurum("YOLDA").build())));
        when(excelService.export(any(), any(), any())).thenReturn(new byte[]{1, 2, 3});

        mockMvc.perform(get("/api/exports/siparis-takip").requestAttr("sirketId", 1L))
                .andExpect(status().isOk());
    }

    @Test
    void teslimatlar_excelDoner() throws Exception {
        when(teslimatService.teslimatlar(any(), any(), any()))
                .thenReturn(List.of(com.raspel.erp.dto.ticaret.TeslimatDTO.builder().id(1L).durum("YOLDA").build()));
        when(excelService.export(any(), any(), any())).thenReturn(new byte[]{1, 2, 3});

        mockMvc.perform(get("/api/exports/teslimatlar").requestAttr("sirketId", 1L).requestAttr("kullaniciId", 2L))
                .andExpect(status().isOk());
    }

    @Test
    void uretimEmirleri_excelDoner() throws Exception {
        when(uretimService.filtreliEmirler(any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(com.raspel.erp.dto.envanter.UretimEmriDTO.builder()
                        .id(1L).urunAd("Masa").durum("TASLAK").build())));
        when(excelService.export(any(), any(), any())).thenReturn(new byte[]{1, 2, 3});

        mockMvc.perform(get("/api/exports/uretim-emirleri").requestAttr("sirketId", 1L))
                .andExpect(status().isOk())
                .andExpect(content().bytes(new byte[]{1, 2, 3}));
    }

    @Test
    void uretimReceteler_excelDoner() throws Exception {
        when(uretimService.receteler(any()))
                .thenReturn(List.of(com.raspel.erp.dto.envanter.ReceteDTO.builder()
                        .id(1L).ad("Masa Reçetesi").urunAd("Masa").build()));
        when(excelService.export(any(), any(), any())).thenReturn(new byte[]{1, 2, 3});

        mockMvc.perform(get("/api/exports/uretim-receteler").requestAttr("sirketId", 1L))
                .andExpect(status().isOk());
    }

    @Test
    void gorunum360StokKar_excelDoner() throws Exception {
        when(gorunum360Service.stokKar360(any(), any(), any()))
                .thenReturn(com.raspel.erp.dto.sistem.Gorunum360DTO.StokKar.builder()
                        .urunler(List.of(com.raspel.erp.dto.sistem.Gorunum360DTO.Satir.builder()
                                .ad("Masa").stokKodu("M-1").build()))
                        .build());
        when(excelService.export(any(), any(), any())).thenReturn(new byte[]{1, 2, 3});

        mockMvc.perform(get("/api/exports/gorunum360-stok-kar").requestAttr("sirketId", 1L))
                .andExpect(status().isOk())
                .andExpect(content().bytes(new byte[]{1, 2, 3}));
    }

    @Test
    void gorunum360Calisan_excelDoner() throws Exception {
        when(gorunum360Service.calisanPerformans360(any()))
                .thenReturn(com.raspel.erp.dto.sistem.Gorunum360DTO.CalisanPerformans.builder()
                        .calisanlar(List.of(com.raspel.erp.dto.sistem.Gorunum360DTO.Calisan.builder()
                                .id(1L).ad("Ali").rol("DRIVER").toplamTeslimat(5).build()))
                        .build());
        when(excelService.export(any(), any(), any())).thenReturn(new byte[]{1, 2, 3});

        mockMvc.perform(get("/api/exports/gorunum360-calisan").requestAttr("sirketId", 1L))
                .andExpect(status().isOk());
    }

    @Test
    void gorunum360Segment_excelDoner() throws Exception {
        when(gorunum360Service.musteriSegmentasyon(any()))
                .thenReturn(com.raspel.erp.dto.sistem.Gorunum360DTO.MusteriSegment.builder()
                        .musteriler(List.of(com.raspel.erp.dto.sistem.Gorunum360DTO.MusteriSegmentSatir.builder()
                                .cariId(1L).ad("Müşteri A").segment("VIP").build()))
                        .build());
        when(excelService.export(any(), any(), any())).thenReturn(new byte[]{1, 2, 3});

        mockMvc.perform(get("/api/exports/gorunum360-segment").requestAttr("sirketId", 1L))
                .andExpect(status().isOk());
    }

    @Test
    void donemler_excelDoner() throws Exception {
        when(donemService.sirketeGoreGetir(any()))
                .thenReturn(List.of(com.raspel.erp.dto.sistem.DonemDTO.builder()
                        .id(1L).ad("2026 Yılı").aktif(true).kilitli(false).build()));
        when(excelService.export(any(), any(), any())).thenReturn(new byte[]{1, 2, 3});

        mockMvc.perform(get("/api/exports/donemler").requestAttr("sirketId", 1L))
                .andExpect(status().isOk());
    }

    @Test
    void donemKapanislar_excelDoner() throws Exception {
        when(donemService.kapanislariGetir(any()))
                .thenReturn(List.of(com.raspel.erp.dto.sistem.DonemKapanisDTO.builder()
                        .id(1L).yil(2025).kilitlenenDonemSayisi(1).build()));
        when(excelService.export(any(), any(), any())).thenReturn(new byte[]{1, 2, 3});

        mockMvc.perform(get("/api/exports/donem-kapanislar").requestAttr("sirketId", 1L))
                .andExpect(status().isOk());
    }

    @Test
    void stokSayim_excelDoner() throws Exception {
        when(stokSayimService.filtreliGetir(any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(com.raspel.erp.dto.envanter.StokSayimDTO.builder()
                        .id(1L).stokAdi("Masa").durum("TASLAK").build())));
        when(excelService.export(any(), any(), any())).thenReturn(new byte[]{1, 2, 3});

        mockMvc.perform(get("/api/exports/stok-sayim").requestAttr("sirketId", 1L))
                .andExpect(status().isOk());
    }

    @Test
    void stokSeri_excelDoner() throws Exception {
        when(stokSeriService.filtreliGetir(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(com.raspel.erp.dto.envanter.StokSeriDTO.builder()
                        .id(1L).stokAdi("Masa").seriNo("SN-1").durum("STOKTA").build())));
        when(excelService.export(any(), any(), any())).thenReturn(new byte[]{1, 2, 3});

        mockMvc.perform(get("/api/exports/stok-seri").requestAttr("sirketId", 1L))
                .andExpect(status().isOk());
    }

    @Test
    void anomaliler_excelDoner() throws Exception {
        when(anomaliTespitEngine.anomalileriTara(any()))
                .thenReturn(List.of(com.raspel.erp.dto.sistem.AnomaliDTO.builder()
                        .id("a1").tur("MUKERRER_FATURA").seviye("YUKSEK").baslik("Mükerrer fatura").build()));
        when(excelService.export(any(), any(), any())).thenReturn(new byte[]{1, 2, 3});

        mockMvc.perform(get("/api/exports/anomaliler").requestAttr("sirketId", 1L))
                .andExpect(status().isOk());
    }

    @Test
    void ipWhitelist_excelDoner() throws Exception {
        when(ipWhitelistService.listele(any()))
                .thenReturn(List.of(Map.of("ipAdresi", "10.0.0.1", "aciklama", "Ofis", "durum", "AKTIF")));
        when(excelService.export(any(), any(), any())).thenReturn(new byte[]{1, 2, 3});

        mockMvc.perform(get("/api/exports/ip-whitelist")
                        .requestAttr("sirketId", 1L)
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
                                .user("admin").roles("ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    void subeler_excelDoner() throws Exception {
        when(subeService.tumunuGetir(any(), any()))
                .thenReturn(new PageImpl<>(List.of(com.raspel.erp.dto.sube.SubeDTO.builder()
                        .id(1L).ad("Merkez").aktif(true).build())));
        when(excelService.export(any(), any(), any())).thenReturn(new byte[]{1, 2, 3});

        mockMvc.perform(get("/api/exports/subeler").requestAttr("sirketId", 1L))
                .andExpect(status().isOk());
    }
}
