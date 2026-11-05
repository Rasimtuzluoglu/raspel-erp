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
import static org.mockito.ArgumentMatchers.eq;
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
    private com.raspel.erp.service.ik.PersonelIzinService personelIzinService;
    @MockBean
    private com.raspel.erp.service.ik.PersonelPuantajService personelPuantajService;
    @MockBean
    private com.raspel.erp.service.ik.VardiyaService vardiyaService;
    @MockBean
    private com.raspel.erp.service.finans.MasrafService masrafService;
    @MockBean
    private com.raspel.erp.service.finans.TaksitService taksitService;
    @MockBean
    private BankaService bankaService;
    @MockBean
    private KasaService kasaService;
    @MockBean
    private AuditLogService auditLogService;
    @MockBean
    private MuhasebeService muhasebeService;
    @MockBean
    private com.raspel.erp.service.muhasebe.IrsaliyeService irsaliyeService;
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
    @MockBean
    private com.raspel.erp.service.sistem.AjandaService ajandaService;
    @MockBean
    private com.raspel.erp.service.finans.ButceService butceService;
    @MockBean
    private com.raspel.erp.service.finans.TahsilatService tahsilatService;
    @MockBean
    private com.raspel.erp.service.finans.CekSenetService cekSenetService;
    @MockBean
    private com.raspel.erp.service.sistem.OnayService onayService;
    @MockBean
    private com.raspel.erp.service.sistem.DashboardService dashboardService;

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
    void onaylar_excelDoner_veTumKolonlariYazar() throws Exception {
        when(onayService.excel(1L, "IZIN", "ayşe"))
                .thenReturn(List.of(com.raspel.erp.dto.sistem.OnayBekleyenDTO.builder()
                        .tur("IZIN").kaynakId(5L).baslik("Ayşe Kaya").altBaslik("YILLIK")
                        .tarih(java.time.LocalDate.now()).durum("BEKLEMEDE")
                        .olusturmaTarihi(java.time.LocalDateTime.now())
                        .aciklama("Yıllık izin").build()));
        when(excelService.export(any(), any(), any())).thenReturn(new byte[]{7, 8, 9});

        mockMvc.perform(get("/api/exports/onaylar")
                        .requestAttr("sirketId", 1L)
                        .param("tur", "IZIN")
                        .param("q", "ayşe")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
                                .user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().bytes(new byte[]{7, 8, 9}));

        org.mockito.Mockito.verify(onayService).excel(1L, "IZIN", "ayşe");
        org.mockito.Mockito.verify(excelService).export(
                org.mockito.ArgumentMatchers.eq("Onaylar"),
                org.mockito.ArgumentMatchers.argThat(cols -> cols != null && cols.length == 9
                        && "Tür".equals(cols[0]) && "Açıklama".equals(cols[8])),
                org.mockito.ArgumentMatchers.argThat(l -> l != null && l.size() == 1
                        && "IZIN".equals(l.get(0).get("Tür"))
                        && "Ayşe Kaya".equals(l.get(0).get("Başlık"))
                        && "BEKLEMEDE".equals(l.get(0).get("Durum"))));
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
    void cekSenet_excelDoner() throws Exception {
        when(cekSenetService.filtreli(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(com.raspel.erp.dto.finans.CekSenetDTO.builder()
                        .id(1L).tur("CEK").cekNo("123").cariHesapAdi("Musteri")
                        .vadeTarihi(java.time.LocalDate.now())
                        .tutar(java.math.BigDecimal.TEN).durum("PORTFOY").build())));
        when(excelService.export(any(), any(), any())).thenReturn(new byte[]{4, 5, 6});

        mockMvc.perform(get("/api/exports/cek-senet").requestAttr("sirketId", 1L))
                .andExpect(status().isOk())
                .andExpect(content().bytes(new byte[]{4, 5, 6}));

        org.mockito.Mockito.verify(excelService).export(
                org.mockito.ArgumentMatchers.eq("CekSenet"),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.argThat(l -> l != null && l.size() == 1
                        && "PORTFOY".equals(l.get(0).get("Durum"))));
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
        when(uretimService.filtreliEmirler(any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(com.raspel.erp.dto.envanter.UretimEmriDTO.builder()
                        .id(1L).urunAd("Masa").durum("TASLAK").build())));
        when(excelService.export(any(), any(), any())).thenReturn(new byte[]{1, 2, 3});

        mockMvc.perform(get("/api/exports/uretim-emirleri").requestAttr("sirketId", 1L))
                .andExpect(status().isOk())
                .andExpect(content().bytes(new byte[]{1, 2, 3}));
    }

    @Test
    void uretimReceteler_excelDoner() throws Exception {
        when(uretimService.recetelerFiltreli(any(), any(), any()))
                .thenReturn(List.of(com.raspel.erp.dto.envanter.ReceteDTO.builder()
                        .id(1L).ad("Masa Reçetesi").urunAd("Masa").build()));
        when(excelService.export(any(), any(), any())).thenReturn(new byte[]{1, 2, 3});

        mockMvc.perform(get("/api/exports/uretim-receteler").requestAttr("sirketId", 1L))
                .andExpect(status().isOk());
    }

    @Test
    void uretimReceteler_excelAramaVeAktiflikParametreleriniIletir() throws Exception {
        when(uretimService.recetelerFiltreli(1L, "masa", true))
                .thenReturn(List.of(com.raspel.erp.dto.envanter.ReceteDTO.builder()
                        .id(2L).ad("Masa Reçetesi").urunAd("Masa").aktif(true).build()));
        when(excelService.export(any(), any(), any())).thenReturn(new byte[]{1, 2, 3});

        mockMvc.perform(get("/api/exports/uretim-receteler")
                        .requestAttr("sirketId", 1L)
                        .param("q", "masa")
                        .param("aktif", "true"))
                .andExpect(status().isOk());

        org.mockito.Mockito.verify(uretimService).recetelerFiltreli(1L, "masa", true);
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
    void izinler_excelDoner() throws Exception {
        when(personelIzinService.filtreli(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(
                        com.raspel.erp.dto.ik.PersonelIzinDTO.builder()
                                .id(1L).personelAdi("Ahmet Yılmaz").izinTuru("YILLIK_IZIN")
                                .durum("BEKLEMEDE").gunSayisi(3).build())));
        when(excelService.export(any(), any(), any())).thenReturn(new byte[]{1, 2, 3});

        mockMvc.perform(get("/api/exports/izinler").requestAttr("sirketId", 1L).param("durum", "BEKLEMEDE"))
                .andExpect(status().isOk())
                .andExpect(content().bytes(new byte[]{1, 2, 3}));
    }

    @Test
    void puantaj_excelDoner() throws Exception {
        when(personelPuantajService.filtreli(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(
                        com.raspel.erp.dto.ik.PersonelPuantajDTO.builder()
                                .id(1L).personelAdi("Ahmet Yılmaz").durum("GELDI").build())));
        when(excelService.export(any(), any(), any())).thenReturn(new byte[]{1, 2, 3});

        mockMvc.perform(get("/api/exports/puantaj").requestAttr("sirketId", 1L).param("durum", "GELDI"))
                .andExpect(status().isOk())
                .andExpect(content().bytes(new byte[]{1, 2, 3}));
    }

    @Test
    void vardiyalar_excelDoner() throws Exception {
        when(vardiyaService.filtreli(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(
                        com.raspel.erp.dto.ik.VardiyaDTO.builder()
                                .id(1L).personelAdi("Ahmet Yılmaz").tur("SABAH").build())));
        when(excelService.export(any(), any(), any())).thenReturn(new byte[]{1, 2, 3});

        mockMvc.perform(get("/api/exports/vardiyalar").requestAttr("sirketId", 1L).param("tur", "SABAH"))
                .andExpect(status().isOk())
                .andExpect(content().bytes(new byte[]{1, 2, 3}));
    }

    @Test
    void masraflar_excelDoner() throws Exception {
        when(masrafService.filtreli(any(), any(), any(), any(), any(), any()))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(
                        com.raspel.erp.dto.finans.MasrafDTO.builder()
                                .id(1L).kategori("OFIS").tutar(new java.math.BigDecimal("750")).build())));
        when(excelService.export(any(), any(), any())).thenReturn(new byte[]{1, 2, 3});

        mockMvc.perform(get("/api/exports/masraflar").requestAttr("sirketId", 1L).param("kategori", "OFIS"))
                .andExpect(status().isOk())
                .andExpect(content().bytes(new byte[]{1, 2, 3}));
    }

    @Test
    void taksitler_excelDoner() throws Exception {
        when(taksitService.listele(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(
                        com.raspel.erp.dto.finans.TaksitDTO.builder()
                                .id(1L).cariAd("Ahmet Yılmaz").tutar(new java.math.BigDecimal("100")).build())));
        when(excelService.export(any(), any(), any())).thenReturn(new byte[]{1, 2, 3});

        mockMvc.perform(get("/api/exports/taksitler").requestAttr("sirketId", 1L).param("durum", "BEKLEMEDE"))
                .andExpect(status().isOk())
                .andExpect(content().bytes(new byte[]{1, 2, 3}));
    }

    @Test
    void butceler_veGerceklesen_excelDoner() throws Exception {
        when(butceService.filtreli(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(com.raspel.erp.dto.finans.ButceDTO.builder()
                        .id(1L).ad("2026 Pazarlama").kategori("Pazarlama").yil(2026).ay(3)
                        .tur("GIDER").tutar(new java.math.BigDecimal("1000")).build())));
        when(butceService.gerceklesen(any(), any(), any(), any()))
                .thenReturn(com.raspel.erp.dto.finans.ButceGerceklesenRaporDTO.builder()
                        .tur("GIDER").yil(2026)
                        .satirlar(List.of(com.raspel.erp.dto.finans.ButceGerceklesenSatirDTO.builder()
                                .kategori("Pazarlama").butce(new java.math.BigDecimal("1000"))
                                .gerceklesen(new java.math.BigDecimal("700")).build()))
                        .build());
        when(excelService.export(any(), any(), any())).thenReturn(new byte[]{1, 2, 3});

        mockMvc.perform(get("/api/exports/butceler").requestAttr("sirketId", 1L).param("yil", "2026"))
                .andExpect(status().isOk())
                .andExpect(content().bytes(new byte[]{1, 2, 3}));

        mockMvc.perform(get("/api/exports/butce-gerceklesen")
                        .requestAttr("sirketId", 1L)
                        .param("yil", "2026")
                        .param("tur", "GIDER"))
                .andExpect(status().isOk())
                .andExpect(content().bytes(new byte[]{1, 2, 3}));
    }

    @Test
    void butceMatris_excelDoner() throws Exception {
        java.math.BigDecimal[] aylar = new java.math.BigDecimal[12];
        java.util.Arrays.fill(aylar, java.math.BigDecimal.ZERO);
        when(butceService.matris(any(), any(), any()))
                .thenReturn(com.raspel.erp.dto.finans.ButceMatrisDTO.builder()
                        .yil(2026).tur("GIDER")
                        .satirlar(List.of(com.raspel.erp.dto.finans.ButceMatrisDTO.Satir.builder()
                                .kategori("Pazarlama").aylar(aylar).toplam(java.math.BigDecimal.ZERO).build()))
                        .aylikToplam(aylar)
                        .genelToplam(java.math.BigDecimal.ZERO)
                        .build());
        when(excelService.export(any(), any(), any())).thenReturn(new byte[]{4, 5, 6});

        mockMvc.perform(get("/api/exports/butce-matris").requestAttr("sirketId", 1L).param("yil", "2026"))
                .andExpect(status().isOk())
                .andExpect(content().bytes(new byte[]{4, 5, 6}));
    }

    @Test
    void ajanda_excelDoner_veTipFiltresiUygular() throws Exception {        when(ajandaService.yaklasan(any(), any(), any(Integer.class)))
                .thenReturn(List.of(
                        com.raspel.erp.dto.sistem.AjandaOlayDTO.builder()
                                .tarih(java.time.LocalDate.now()).tip("VADE").baslik("Fatura vadesi")
                                .kaynakTip("FATURA").kaynakNo("FTR-1").build(),
                        com.raspel.erp.dto.sistem.AjandaOlayDTO.builder()
                                .tarih(java.time.LocalDate.now()).tip("KISISEL_GOREV").baslik("Görev")
                                .kaynakTip("KISISEL_GOREV").build()));
        when(excelService.export(any(), any(), any())).thenReturn(new byte[]{1, 2, 3});

        mockMvc.perform(get("/api/exports/ajanda")
                        .requestAttr("sirketId", 1L)
                        .requestAttr("kullaniciId", 7L)
                        .param("gun", "30")
                        .param("tip", "VADE"))
                .andExpect(status().isOk())
                .andExpect(content().bytes(new byte[]{1, 2, 3}));

        org.mockito.Mockito.verify(excelService).export(
                org.mockito.ArgumentMatchers.eq("Ajanda"),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.argThat(l -> l != null && l.size() == 1));
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

    @Test
    void irsaliyeler_excelDoner() throws Exception {
        var dto = com.raspel.erp.dto.muhasebe.IrsaliyeDTO.builder()
                .id(1L).irsaliyeNo("IRS-1").tarih(java.time.LocalDate.now())
                .cariHesapAdi("Müşteri A").tur("SATIS").durum("SEVK_EDILDI")
                .plaka("34ABC123").soforAd("Ali Veli").faturaNumarasi("FTR-1")
                .kalemler(List.of()).build();
        when(irsaliyeService.filtreli(any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(dto)));
        when(excelService.export(any(), any(), any())).thenReturn(new byte[]{1, 2, 3});

        mockMvc.perform(get("/api/exports/irsaliyeler")
                        .requestAttr("sirketId", 1L)
                        .param("durum", "SEVK_EDILDI")
                        .param("faturali", "true"))
                .andExpect(status().isOk())
                .andExpect(content().bytes(new byte[]{1, 2, 3}));

        org.mockito.Mockito.verify(irsaliyeService).filtreli(
                org.mockito.ArgumentMatchers.eq(1L),
                org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.eq("SEVK_EDILDI"),
                org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.eq(Boolean.TRUE),
                any(org.springframework.data.domain.Pageable.class));
        org.mockito.Mockito.verify(excelService).export(
                org.mockito.ArgumentMatchers.eq("Irsaliyeler"),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.argThat(l -> l != null && l.size() == 1
                        && "IRS-1".equals(l.get(0).get("No"))
                        && "Ali Veli".equals(l.get(0).get("Şoför"))));
    }

    @Test
    void tahsilatGecmis_islemVeDagilimSatirlariYazar() throws Exception {
        var islem = com.raspel.erp.dto.finans.TahsilatIslemDTO.builder()
                .id(5L).tarih(java.time.LocalDate.now()).cariId(1L).cariAd("Müşteri A")
                .tutar(new java.math.BigDecimal("600.00")).odemeYontemi("NAKIT")
                .kasaAd("Merkez Kasa").durum("AKTIF")
                .dagilimlar(List.of(
                        com.raspel.erp.dto.finans.TahsilatIslemDTO.Dagilim.builder()
                                .hareketId(10L).faturaId(3L).faturaNumarasi("FTR-3")
                                .tutar(new java.math.BigDecimal("500.00")).aciklama("Tahsilat").iptal(false)
                                .build(),
                        com.raspel.erp.dto.finans.TahsilatIslemDTO.Dagilim.builder()
                                .hareketId(11L).tutar(new java.math.BigDecimal("100.00"))
                                .aciklama("Avans").iptal(true).build()))
                .build();
        when(tahsilatService.gecmisFiltreli(any(), any(), any(), any(), any(), any(), any(),
                any(), any(), any())).thenReturn(new PageImpl<>(List.of(islem)));
        when(excelService.export(any(), any(), any())).thenReturn(new byte[]{1, 2, 3});

        mockMvc.perform(get("/api/exports/tahsilat-gecmis")
                        .requestAttr("sirketId", 1L)
                        .param("durum", "AKTIF"))
                .andExpect(status().isOk())
                .andExpect(content().bytes(new byte[]{1, 2, 3}));

        org.mockito.Mockito.verify(excelService).export(
                org.mockito.ArgumentMatchers.eq("TahsilatGecmis"),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.argThat(l -> l != null && l.size() == 2
                        && "FTR-3".equals(l.get(0).get("Fatura No"))
                        && "Evet".equals(l.get(1).get("Dağıtım İptal"))));
    }

    @Test
    void tahsilatYaslandirma_filtreliFaturalariYazar() throws Exception {
        var cari = com.raspel.erp.dto.finans.TahsilatDTO.CariOzet.builder()
                .cariId(1L).cariAd("Müşteri A").telefon("0555").email("a@x.com")
                .aralik("0-30 Gün")
                .faturalar(List.of(com.raspel.erp.dto.finans.TahsilatDTO.FaturaOzet.builder()
                        .faturaId(3L).faturaNumarasi("FTR-3")
                        .vadeTarihi(java.time.LocalDate.now().minusDays(5))
                        .kalanTutar(new java.math.BigDecimal("500.00")).gecikmeGunu(5).build()))
                .build();
        when(tahsilatService.ozetGetir(eq(1L), eq("müşteri"), eq("0-30"), eq(true),
                eq(new java.math.BigDecimal("100.00"))))
                .thenReturn(com.raspel.erp.dto.finans.TahsilatDTO.builder()
                        .cariler(List.of(cari)).build());
        when(excelService.export(any(), any(), any())).thenReturn(new byte[]{1, 2, 3});

        mockMvc.perform(get("/api/exports/tahsilat-yaslandirma")
                        .requestAttr("sirketId", 1L)
                        .param("q", "müşteri")
                        .param("aralik", "0-30")
                        .param("sadeceGecikmis", "true")
                        .param("minTutar", "100.00"))
                .andExpect(status().isOk())
                .andExpect(content().bytes(new byte[]{1, 2, 3}));

        org.mockito.Mockito.verify(excelService).export(
                org.mockito.ArgumentMatchers.eq("TahsilatYaslandirma"),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.argThat(l -> l != null && l.size() == 1
                        && "FTR-3".equals(l.get(0).get("Fatura No"))));
    }

    @Test
    void dashboard_ozetVeGrafiklerSayfalariniDoner() throws Exception {
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        "admin", null,
                        List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ADMIN"))));
        var ozet = com.raspel.erp.dto.sistem.DashboardOzetDTO.builder()
                .donem(com.raspel.erp.dto.sistem.DashboardOzetDTO.DonemDTO.builder()
                        .kod("BU_YIL").etiket("2026 Yılı").build())
                .kpi(com.raspel.erp.dto.sistem.DashboardOzetDTO.KpiDTO.builder()
                        .ciro(com.raspel.erp.dto.sistem.DashboardOzetDTO.CiroDTO.builder()
                                .deger(new java.math.BigDecimal("12345.67"))
                                .onceki(new java.math.BigDecimal("10000.00"))
                                .degisimYuzde(new java.math.BigDecimal("23.46"))
                                .build())
                        .kar(com.raspel.erp.dto.sistem.DashboardOzetDTO.KarDTO.builder()
                                .deger(new java.math.BigDecimal("2345.67"))
                                .onceki(new java.math.BigDecimal("1000.00"))
                                .build())
                        .likidite(com.raspel.erp.dto.sistem.DashboardOzetDTO.LikiditeDTO.builder()
                                .deger(new java.math.BigDecimal("5000.00")).build())
                        .netCari(com.raspel.erp.dto.sistem.DashboardOzetDTO.NetCariDTO.builder()
                                .alacak(new java.math.BigDecimal("8000.00"))
                                .borc(new java.math.BigDecimal("3000.00")).build())
                        .build())
                .stok(com.raspel.erp.dto.sistem.DashboardOzetDTO.StokDTO.builder()
                        .deger(new java.math.BigDecimal("15000.00")).kritikAdet(4L).build())
                .gorevSayilari(com.raspel.erp.dto.sistem.DashboardOzetDTO.GorevSayilariDTO.builder()
                        .onayBekleyen(3L).vadesiGecen(2L).build())
                .build();
        var grafikler = com.raspel.erp.dto.sistem.DashboardGrafikDTO.builder()
                .aylikGelirGider(List.of(com.raspel.erp.dto.sistem.DashboardDTO.AylikGelirGiderDTO.builder()
                        .ay("2026-01").gelir(new java.math.BigDecimal("100"))
                        .gider(new java.math.BigDecimal("40")).build()))
                .kategoriSatislari(List.of(com.raspel.erp.dto.sistem.DashboardDTO.KategoriSatisDTO.builder()
                        .kategori("Mobilya").tutar(new java.math.BigDecimal("900")).build()))
                .build();

        when(dashboardService.ozetGetir(eq(1L), eq(7L), any(), eq("ADMIN"), eq("BU_YIL"))).thenReturn(ozet);
        when(dashboardService.grafiklerGetir(1L)).thenReturn(grafikler);
        when(excelService.exportCokluSayfa(any())).thenAnswer(cagri -> {
            @SuppressWarnings("unchecked")
            List<ExcelExportService.Sayfa> sayfalar = (List<ExcelExportService.Sayfa>) cagri.getArgument(0);
            return new ExcelExportService().exportCokluSayfa(sayfalar);
        });

        byte[] govde = mockMvc.perform(get("/api/exports/dashboard")
                        .requestAttr("sirketId", 1L)
                        .requestAttr("kullaniciId", 7L)
                        .requestAttr("displayName", "Ayşe")
                        .param("donem", "BU_YIL"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header()
                        .string(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                                org.hamcrest.Matchers.containsString("attachment; filename=\"dashboard-"
                                        + java.time.LocalDate.now()
                                        .format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE)
                                        + ".xlsx\"")))
                .andReturn().getResponse().getContentAsByteArray();

        try (org.apache.poi.xssf.usermodel.XSSFWorkbook wb =
                     new org.apache.poi.xssf.usermodel.XSSFWorkbook(new java.io.ByteArrayInputStream(govde))) {
            var ozetSayfa = wb.getSheet("Ozet");
            var grafikSayfa = wb.getSheet("Grafikler");
            org.junit.jupiter.api.Assertions.assertNotNull(ozetSayfa);
            org.junit.jupiter.api.Assertions.assertNotNull(grafikSayfa);

            org.junit.jupiter.api.Assertions.assertEquals("Alan", ozetSayfa.getRow(0).getCell(0).getStringCellValue());
            org.junit.jupiter.api.Assertions.assertEquals("Dönem", ozetSayfa.getRow(1).getCell(0).getStringCellValue());
            org.junit.jupiter.api.Assertions.assertEquals("2026 Yılı", ozetSayfa.getRow(1).getCell(1).getStringCellValue());
            org.junit.jupiter.api.Assertions.assertEquals("Ciro", ozetSayfa.getRow(2).getCell(0).getStringCellValue());
            org.junit.jupiter.api.Assertions.assertEquals(12345.67,
                    ozetSayfa.getRow(2).getCell(1).getNumericCellValue(), 0.001);
            org.junit.jupiter.api.Assertions.assertEquals("Kritik Stok",
                    ozetSayfa.getRow(11).getCell(0).getStringCellValue());
            org.junit.jupiter.api.Assertions.assertEquals(4.0,
                    ozetSayfa.getRow(11).getCell(1).getNumericCellValue(), 0.001);

            org.junit.jupiter.api.Assertions.assertEquals("12 Ay Gelir/Gider/Net",
                    grafikSayfa.getRow(0).getCell(0).getStringCellValue());
            org.junit.jupiter.api.Assertions.assertEquals("Ay", grafikSayfa.getRow(1).getCell(0).getStringCellValue());
            org.junit.jupiter.api.Assertions.assertEquals("2026-01",
                    grafikSayfa.getRow(2).getCell(0).getStringCellValue());
            org.junit.jupiter.api.Assertions.assertEquals(100.0,
                    grafikSayfa.getRow(2).getCell(1).getNumericCellValue(), 0.001);
            org.junit.jupiter.api.Assertions.assertEquals(60.0,
                    grafikSayfa.getRow(2).getCell(3).getNumericCellValue(), 0.001);
            // Bölüm başlıkları arasında boş satır bulunur (hücresiz satır).
            org.junit.jupiter.api.Assertions.assertNull(grafikSayfa.getRow(3).getCell(0));
        }

        org.mockito.Mockito.verify(dashboardService).ozetGetir(
                org.mockito.ArgumentMatchers.eq(1L), org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.eq("Ayşe"), org.mockito.ArgumentMatchers.eq("ADMIN"),
                org.mockito.ArgumentMatchers.eq("BU_YIL"));
        org.mockito.Mockito.verify(dashboardService).grafiklerGetir(1L);
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
    }
}
