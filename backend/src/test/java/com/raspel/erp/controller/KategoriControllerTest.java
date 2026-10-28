package com.raspel.erp.controller;

import com.raspel.erp.dto.sistem.*;
import com.raspel.erp.entity.sistem.KategoriKapsam;
import com.raspel.erp.exception.BusinessException;
import com.raspel.erp.service.sistem.KategoriService;
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

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.raspel.erp.controller.sistem.KategoriController;

@WebMvcTest(KategoriController.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@AutoConfigureMockMvc(addFilters = false)
@Import(TestSecurityMocks.class)
class KategoriControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private KategoriService kategoriService;

    @Test
    void shouldGetAll() throws Exception {
        var list = List.of(KategoriDTO.builder()
                .id(1L).ad("Satış").kapsam("KASA_GELIR").tur("GELIR").build());
        when(kategoriService.listele(eq(1L), isNull(), isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(list));

        mockMvc.perform(get("/api/kategoriler").requestAttr("sirketId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].ad").value("Satış"));
    }

    @Test
    void shouldFilterByKapsam() throws Exception {
        when(kategoriService.listele(eq(1L), eq(KategoriKapsam.STOK), eq("gıd"), eq(true), isNull(),
                any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(
                        KategoriDTO.builder().id(2L).ad("Gıda").kapsam("STOK").build())));

        mockMvc.perform(get("/api/kategoriler")
                        .requestAttr("sirketId", 1L)
                        .param("kapsam", "STOK")
                        .param("q", "gıd")
                        .param("aktif", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].kapsam").value("STOK"));
    }

    @Test
    void shouldRejectInvalidKapsam() throws Exception {
        mockMvc.perform(get("/api/kategoriler").requestAttr("sirketId", 1L).param("kapsam", "YOK"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldGetByTur() throws Exception {
        when(kategoriService.turuGetir("GIDER", 1L)).thenReturn(List.of(
                KategoriDTO.builder().id(1L).ad("Maaş").kapsam("KASA_GIDER").tur("GIDER").build()));

        mockMvc.perform(get("/api/kategoriler/tur/GIDER").requestAttr("sirketId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].ad").value("Maaş"))
                .andExpect(jsonPath("$[0].tur").value("GIDER"));
    }

    @Test
    void shouldGetAgac() throws Exception {
        when(kategoriService.agac(1L, KategoriKapsam.STOK, null, null)).thenReturn(List.of(
                KategoriAgacDTO.builder().id(1L).ad("Gıda").kapsam("STOK")
                        .altKategoriler(List.of(KategoriAgacDTO.builder().id(2L).ad("Süt").build()))
                        .build()));

        mockMvc.perform(get("/api/kategoriler/agac")
                        .requestAttr("sirketId", 1L)
                        .param("kapsam", "STOK"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].ad").value("Gıda"))
                .andExpect(jsonPath("$[0].altKategoriler[0].ad").value("Süt"));
    }

    @Test
    void shouldGetOzet() throws Exception {
        when(kategoriService.ozet(1L, KategoriKapsam.STOK))
                .thenReturn(new KategoriOzetDTO(10, 7, 3, 2));

        mockMvc.perform(get("/api/kategoriler/ozet")
                        .requestAttr("sirketId", 1L)
                        .param("kapsam", "STOK"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.toplam").value(10))
                .andExpect(jsonPath("$.pasif").value(3));
    }

    @Test
    void shouldGetSecenekler() throws Exception {
        when(kategoriService.secenekler(1L, KategoriKapsam.KASA_GELIR)).thenReturn(List.of(
                KategoriAgacDTO.builder().id(1L).ad("Satış").build()));

        mockMvc.perform(get("/api/kategoriler/secenekler")
                        .requestAttr("sirketId", 1L)
                        .param("kapsam", "KASA_GELIR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].ad").value("Satış"));
    }

    @Test
    void shouldGetSistemKategorileri() throws Exception {
        when(kategoriService.sistemKategorileri()).thenReturn(List.of(
                new SistemKategoriDTO("YAKIT", "Yakıt", "MASRAF")));

        mockMvc.perform(get("/api/kategoriler/sistem-kategorileri"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].kod").value("YAKIT"));
    }

    @Test
    void shouldGetKullanim() throws Exception {
        when(kategoriService.kullanim(1L)).thenReturn(KategoriKullanimDTO.builder()
                .kategoriId(1L).ad("Gıda").toplam(4)
                .detaylar(List.of(new KategoriKullanimDTO.Detay("STOK", 4)))
                .build());

        mockMvc.perform(get("/api/kategoriler/1/kullanim"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.toplam").value(4))
                .andExpect(jsonPath("$.detaylar[0].modul").value("STOK"));
    }

    @Test
    void shouldCreate() throws Exception {
        var dto = KategoriDTO.builder().id(1L).ad("Yeni Kategori").kapsam("STOK").build();
        when(kategoriService.olustur(any(KategoriDTO.class), eq(1L))).thenReturn(dto);

        mockMvc.perform(post("/api/kategoriler")
                        .requestAttr("sirketId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ad").value("Yeni Kategori"));
    }

    @Test
    void shouldRejectCreateWithoutAd() throws Exception {
        var dto = KategoriDTO.builder().kapsam("STOK").build();

        mockMvc.perform(post("/api/kategoriler")
                        .requestAttr("sirketId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
        verify(kategoriService, never()).olustur(any(), any());
    }

    @Test
    void shouldUpdate() throws Exception {
        var dto = KategoriDTO.builder().id(1L).ad("Güncel").kapsam("STOK").build();
        when(kategoriService.guncelle(eq(1L), any(KategoriDTO.class))).thenReturn(dto);

        mockMvc.perform(put("/api/kategoriler/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ad").value("Güncel"));
    }

    @Test
    void shouldDelete() throws Exception {
        doNothing().when(kategoriService).sil(1L);

        mockMvc.perform(delete("/api/kategoriler/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldArchive() throws Exception {
        when(kategoriService.arsivle(1L)).thenReturn(
                KategoriDTO.builder().id(1L).ad("Gıda").aktif(false).build());

        mockMvc.perform(post("/api/kategoriler/1/arsivle"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.aktif").value(false));
    }

    @Test
    void shouldActivate() throws Exception {
        when(kategoriService.aktiflestir(1L)).thenReturn(
                KategoriDTO.builder().id(1L).ad("Gıda").aktif(true).build());

        mockMvc.perform(post("/api/kategoriler/1/aktiflestir"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.aktif").value(true));
    }

    @Test
    void shouldMerge() throws Exception {
        when(kategoriService.birlestir(1L, 2L)).thenReturn(
                KategoriDTO.builder().id(2L).ad("Teknoloji").build());

        mockMvc.perform(post("/api/kategoriler/birlestir")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"kaynakId\":1,\"hedefId\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ad").value("Teknoloji"));
    }

    @Test
    void shouldReturn400WhenMergeRejected() throws Exception {
        when(kategoriService.birlestir(1L, 2L))
                .thenThrow(new BusinessException("Farklı kapsamdaki kategoriler birleştirilemez"));

        mockMvc.perform(post("/api/kategoriler/birlestir")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"kaynakId\":1,\"hedefId\":2}"))
                .andExpect(status().isBadRequest());
    }
}
