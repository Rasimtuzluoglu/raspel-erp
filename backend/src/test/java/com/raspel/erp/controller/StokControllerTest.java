package com.raspel.erp.controller;

import com.raspel.erp.dto.envanter.StokDTO;
import com.raspel.erp.dto.envanter.StokHareketDTO;
import com.raspel.erp.exception.ResourceNotFoundException;
import com.raspel.erp.service.envanter.StokService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
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
import com.raspel.erp.entity.envanter.Stok;
import com.raspel.erp.dto.envanter.AlisOzetDTO;
import com.raspel.erp.dto.envanter.KarlilikDTO;
import com.raspel.erp.service.envanter.StokAnalizService;
import com.raspel.erp.controller.envanter.StokController;

@WebMvcTest(StokController.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@AutoConfigureMockMvc(addFilters = false)
@Import(TestSecurityMocks.class)
class StokControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    
    @MockBean
    private StokService stokService;

    @MockBean
    private StokAnalizService stokAnalizService;

    @MockBean
    private com.raspel.erp.service.sistem.QRService qrService;

    @MockBean
    private com.raspel.erp.service.sistem.PdfRaporService pdfRaporService;

    @Test
    void shouldGetAll() throws Exception {
        var list = List.of(StokDTO.builder().id(1L).ad("Kalem").stokKodu("KLM001").fiyat(BigDecimal.valueOf(10)).build());
        when(stokService.tumunuGetir(anyLong(), any())).thenReturn(new org.springframework.data.domain.PageImpl<>(list));

        mockMvc.perform(get("/api/stoklar").requestAttr("sirketId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].ad").value("Kalem"));
    }

    @Test
    void shouldGetByBarkod() throws Exception {
        StokDTO dto = StokDTO.builder().id(5L).ad("Kalem").barkod("BAR123").build();
        when(stokService.barkodIleBul("BAR123", 1L)).thenReturn(dto);

        mockMvc.perform(get("/api/stoklar/barkod/BAR123").requestAttr("sirketId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.barkod").value("BAR123"));
    }

    @Test
    void shouldGetByBarkodNotFound() throws Exception {
        when(stokService.barkodIleBul("YOK", 1L)).thenReturn(null);

        mockMvc.perform(get("/api/stoklar/barkod/YOK").requestAttr("sirketId", 1L))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldGetEtiketPdf() throws Exception {
        Stok stok = Stok.builder().id(5L).ad("Kalem").barkod("BAR123").build();
        when(stokService.entityGetir(5L)).thenReturn(stok);
        when(qrService.qrPng(anyString(), anyInt())).thenReturn(new byte[] {1, 2, 3});
        when(pdfRaporService.stokEtiketi(any(Stok.class), any())).thenReturn(new byte[] {0x25, 0x50, 0x44, 0x46});

        mockMvc.perform(get("/api/stoklar/5/etiket"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PDF));
    }

    @Test
    void shouldSearch() throws Exception {
        var list = List.of(StokDTO.builder().id(1L).ad("Kalem").build());
        when(stokService.ara("Kalem", 1L)).thenReturn(list);

        mockMvc.perform(get("/api/stoklar/ara").param("q", "Kalem").requestAttr("sirketId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].ad").value("Kalem"));
    }

    @Test
    void shouldGetById() throws Exception {
        var dto = StokDTO.builder().id(1L).ad("Kalem").stokKodu("KLM001").build();
        when(stokService.getir(1L)).thenReturn(dto);

        mockMvc.perform(get("/api/stoklar/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stokKodu").value("KLM001"));
    }

    @Test
    void shouldReturnNotFoundWhenGetById() throws Exception {
        when(stokService.getir(anyLong())).thenThrow(new ResourceNotFoundException("Stok", 999L));

        mockMvc.perform(get("/api/stoklar/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldCreate() throws Exception {
        var dto = StokDTO.builder().id(1L).ad("Yeni Ürün").fiyat(BigDecimal.valueOf(25)).build();
        when(stokService.olustur(any(StokDTO.class), anyLong())).thenReturn(dto);

        mockMvc.perform(post("/api/stoklar")
                        .requestAttr("sirketId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ad").value("Yeni Ürün"));
    }

    @Test
    void shouldUpdate() throws Exception {
        var dto = StokDTO.builder().id(1L).ad("Güncel Ürün").fiyat(BigDecimal.valueOf(30)).build();
        when(stokService.guncelle(eq(1L), any(StokDTO.class))).thenReturn(dto);

        mockMvc.perform(put("/api/stoklar/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ad").value("Güncel Ürün"));
    }

    @Test
    void shouldDelete() throws Exception {
        doNothing().when(stokService).sil(1L);

        mockMvc.perform(delete("/api/stoklar/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldGetHareketler() throws Exception {
        var list = List.of(StokHareketDTO.builder().id(1L).tur("GIRIS").miktar(BigDecimal.valueOf(10)).build());
        when(stokService.hareketler(1L)).thenReturn(list);

        mockMvc.perform(get("/api/stoklar/1/hareketler"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tur").value("GIRIS"));
    }

    @Test
    void shouldGetAllHareketler() throws Exception {
        var list = List.of(StokHareketDTO.builder().id(1L).tur("CIKIS").miktar(BigDecimal.valueOf(5)).build());
        when(stokService.tumHareketler(anyLong())).thenReturn(list);

        mockMvc.perform(get("/api/stoklar/hareketler/tum").requestAttr("sirketId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tur").value("CIKIS"));
    }

    @Test
    void shouldAddHareket() throws Exception {
        var dto = StokHareketDTO.builder().id(1L).stokId(1L).tur("GIRIS").miktar(BigDecimal.valueOf(10)).hareketTarihi(LocalDate.now()).build();
        when(stokService.hareketEkle(any(StokHareketDTO.class))).thenReturn(dto);

        mockMvc.perform(post("/api/stoklar/1/hareketler")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tur").value("GIRIS"));
    }

    @Test
    void shouldDeleteHareket() throws Exception {
        doNothing().when(stokService).hareketSil(1L);

        mockMvc.perform(delete("/api/stoklar/hareketler/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldGetTalepTahmini() throws Exception {
        var list = List.of(com.raspel.erp.dto.envanter.TalepTahminiDTO.builder()
                .stokId(1L)
                .ad("MDF Panel")
                .tahminiTukenmeGunu(12)
                .durum("DIKKAT")
                .build());
        when(stokService.talepTahmini(eq(1L))).thenReturn(list);

        mockMvc.perform(get("/api/stoklar/talep-tahmini").requestAttr("sirketId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].ad").value("MDF Panel"))
                .andExpect(jsonPath("$[0].durum").value("DIKKAT"));
    }

    @Test
    void shouldGetAlisOzet() throws Exception {
        var dto = AlisOzetDTO.builder().stokId(1L)
                .toplamAlisMiktar(BigDecimal.valueOf(30))
                .ortalamaBirimFiyat(BigDecimal.valueOf(133.33)).build();
        when(stokAnalizService.alisOzet(eq(1L), eq(1L), any(), any())).thenReturn(dto);

        mockMvc.perform(get("/api/stoklar/1/alis-ozet").requestAttr("sirketId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.toplamAlisMiktar").value(30))
                .andExpect(jsonPath("$.ortalamaBirimFiyat").value(133.33));
    }

    @Test
    void shouldGetKarlilik() throws Exception {
        var dto = KarlilikDTO.builder().stokId(1L)
                .toplamBrutKar(BigDecimal.valueOf(100)).brutKarMarji(BigDecimal.valueOf(33.33)).build();
        when(stokAnalizService.karlilik(eq(1L), eq(1L), any(), any())).thenReturn(dto);

        mockMvc.perform(get("/api/stoklar/1/karlilik").requestAttr("sirketId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.toplamBrutKar").value(100))
                .andExpect(jsonPath("$.brutKarMarji").value(33.33));
    }
}




