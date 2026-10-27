package com.raspel.erp.controller.finans;

import com.raspel.erp.controller.TestSecurityMocks;
import com.raspel.erp.dto.finans.ButceDTO;
import com.raspel.erp.service.finans.ButceService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ButceController.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@AutoConfigureMockMvc(addFilters = false)
@Import(TestSecurityMocks.class)
class ButceControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private ButceService butceService;
    @MockBean private com.raspel.erp.service.sistem.RaporService raporService;

    private ButceDTO ornek() {
        return ButceDTO.builder().id(1L).ad("2026 Bütçe").yil(2026).ay(1)
                .tutar(new BigDecimal("50000")).tur("GIDER").build();
    }

    @Test
    void shouldGetAll() throws Exception {
        when(butceService.tumunuGetir(anyLong(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(ornek())));
        mockMvc.perform(get("/api/butceler").requestAttr("sirketId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].ad").value("2026 Bütçe"));
    }

    @Test
    void shouldGetById() throws Exception {
        when(butceService.getir(1L)).thenReturn(ornek());
        mockMvc.perform(get("/api/butceler/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.yil").value(2026));
    }

    @Test
    void filtreli_parametreleriIletirVeSayfaliDoner() throws Exception {
        when(butceService.filtreli(eq(1L), eq(2026), eq(3), eq("GIDER"), eq("Pazarlama"),
                isNull(), eq("kampanya"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(ornek())));

        mockMvc.perform(get("/api/butceler/filtreli")
                        .requestAttr("sirketId", 1L)
                        .param("yil", "2026")
                        .param("ay", "3")
                        .param("tur", "GIDER")
                        .param("kategori", "Pazarlama")
                        .param("q", "kampanya"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].ad").value("2026 Bütçe"));
    }

    @Test
    void ozet_veKategorilerDoner() throws Exception {
        when(butceService.ozet(1L, 2026, "GIDER")).thenReturn(
                com.raspel.erp.dto.finans.ButceOzetDTO.builder()
                        .butceToplam(new BigDecimal("5000"))
                        .gerceklesenToplam(new BigDecimal("4000"))
                        .sapma(new BigDecimal("-1000"))
                        .kullanimYuzde(new BigDecimal("80.0"))
                        .kayitSayisi(3)
                        .build());
        when(butceService.kategoriler(1L, 2026, "GIDER")).thenReturn(List.of("Pazarlama", "Kira"));

        mockMvc.perform(get("/api/butceler/ozet")
                        .requestAttr("sirketId", 1L)
                        .param("yil", "2026")
                        .param("tur", "GIDER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.butceToplam").value(5000))
                .andExpect(jsonPath("$.kullanimYuzde").value(80.0));

        mockMvc.perform(get("/api/butceler/kategoriler")
                        .requestAttr("sirketId", 1L)
                        .param("yil", "2026")
                        .param("tur", "GIDER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[1]").value("Kira"));
    }

    @Test
    void shouldCreate() throws Exception {        when(butceService.olustur(any(ButceDTO.class), anyLong())).thenReturn(ornek());
        mockMvc.perform(post("/api/butceler")
                        .requestAttr("sirketId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ornek())))
                .andExpect(status().isCreated());
    }

    @Test
    void shouldUpdate() throws Exception {
        when(butceService.guncelle(eq(1L), any(ButceDTO.class))).thenReturn(ornek());
        mockMvc.perform(put("/api/butceler/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ornek())))
                .andExpect(status().isOk());
    }

    @Test
    void shouldDelete() throws Exception {
        doNothing().when(butceService).sil(1L);
        mockMvc.perform(delete("/api/butceler/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void gerceklesen_turAyrimliRaporDoner() throws Exception {
        when(butceService.gerceklesen(1L, 2026, 3, "GIDER")).thenReturn(
                com.raspel.erp.dto.finans.ButceGerceklesenRaporDTO.builder()
                        .tur("GIDER").yil(2026).ay(3)
                        .butceToplam(new BigDecimal("1500"))
                        .gerceklesenToplam(new BigDecimal("1000"))
                        .sapma(new BigDecimal("-500"))
                        .kullanimYuzde(new BigDecimal("66.7"))
                        .satirlar(List.of(com.raspel.erp.dto.finans.ButceGerceklesenSatirDTO.builder()
                                .kategori("Pazarlama").butce(new BigDecimal("1000"))
                                .gerceklesen(new BigDecimal("700")).build()))
                        .build());

        mockMvc.perform(get("/api/butceler/gerceklesen")
                        .requestAttr("sirketId", 1L)
                        .param("yil", "2026")
                        .param("ay", "3")
                        .param("tur", "GIDER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tur").value("GIDER"))
                .andExpect(jsonPath("$.satirlar[0].kategori").value("Pazarlama"));
    }

    @Test
    void matris_veKopyala_calisir() throws Exception {        java.math.BigDecimal[] aylar = new java.math.BigDecimal[12];
        java.util.Arrays.fill(aylar, java.math.BigDecimal.ZERO);
        aylar[0] = new BigDecimal("100");
        when(butceService.matris(1L, 2026, "GIDER")).thenReturn(
                com.raspel.erp.dto.finans.ButceMatrisDTO.builder()
                        .yil(2026).tur("GIDER")
                        .satirlar(List.of(com.raspel.erp.dto.finans.ButceMatrisDTO.Satir.builder()
                                .kategori("Pazarlama").aylar(aylar).toplam(new BigDecimal("100")).build()))
                        .aylikToplam(aylar)
                        .genelToplam(new BigDecimal("100"))
                        .build());
        when(butceService.kopyala(1L, 2025, 2026, "GIDER"))
                .thenReturn(java.util.Map.of("kopyalanan", 4, "atlanan", 2));

        mockMvc.perform(get("/api/butceler/matris")
                        .requestAttr("sirketId", 1L)
                        .param("yil", "2026")
                        .param("tur", "GIDER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.satirlar[0].kategori").value("Pazarlama"));

        mockMvc.perform(post("/api/butceler/kopyala")
                        .requestAttr("sirketId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"kaynakYil\":2025,\"hedefYil\":2026,\"tur\":\"GIDER\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kopyalanan").value(4));
    }

    @Test
    void gerceklesenPdf_pdfDoner() throws Exception {        when(butceService.gerceklesen(1L, 2026, null, "GELIR")).thenReturn(
                com.raspel.erp.dto.finans.ButceGerceklesenRaporDTO.builder()
                        .tur("GELIR").yil(2026)
                        .satirlar(List.of())
                        .build());
        when(raporService.butceGerceklesenPdf(any(), any(), eq(2026), isNull()))
                .thenReturn(new byte[]{37, 80, 68, 70});

        mockMvc.perform(get("/api/butceler/gerceklesen/pdf")
                        .requestAttr("sirketId", 1L)
                        .param("yil", "2026")
                        .param("tur", "GELIR"))
                .andExpect(status().isOk())
                .andExpect(content().bytes(new byte[]{37, 80, 68, 70}));
    }

    @Test
    void onayAkisi_veRevizyonGecmisiDoner() throws Exception {
        when(butceService.onayla(1L, 7L)).thenReturn(ButceDTO.builder()
                .id(1L).ad("Bütçe").durum("ONAYLANDI").revizyon(2).build());
        when(butceService.reddet(1L, "Fazla", 7L)).thenReturn(ButceDTO.builder()
                .id(1L).ad("Bütçe").durum("REDDEDILDI").redGerekcesi("Fazla").build());
        when(butceService.onayBekleyenler(1L, 2026)).thenReturn(List.of(
                ButceDTO.builder().id(2L).ad("Taslak").durum("TASLAK").build()));
        when(butceService.revizyonlar(1L)).thenReturn(List.of(
                com.raspel.erp.dto.finans.ButceRevizyonDTO.builder()
                        .id(1L).butceId(1L).revizyon(1).islem("OLUSTUR").build()));

        mockMvc.perform(put("/api/butceler/1/onayla")
                        .requestAttr("sirketId", 1L)
                        .requestAttr("kullaniciId", 7L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.durum").value("ONAYLANDI"));

        mockMvc.perform(put("/api/butceler/1/reddet")
                        .requestAttr("sirketId", 1L)
                        .requestAttr("kullaniciId", 7L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"gerekce\":\"Fazla\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.durum").value("REDDEDILDI"));

        mockMvc.perform(get("/api/butceler/onay-bekleyen")
                        .requestAttr("sirketId", 1L)
                        .param("yil", "2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].durum").value("TASLAK"));

        mockMvc.perform(get("/api/butceler/1/revizyonlar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].islem").value("OLUSTUR"));
    }
}
