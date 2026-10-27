package com.raspel.erp.controller;

import com.raspel.erp.dto.muhasebe.IrsaliyeDTO;
import com.raspel.erp.service.muhasebe.IrsaliyeService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import com.raspel.erp.exception.ResourceNotFoundException;
import org.springframework.test.annotation.DirtiesContext;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.raspel.erp.controller.muhasebe.IrsaliyeController;

@WebMvcTest(IrsaliyeController.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@AutoConfigureMockMvc(addFilters = false)
@Import(TestSecurityMocks.class)
class IrsaliyeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    
    @MockBean
    private IrsaliyeService irsaliyeService;

    @Test
    void shouldGetAll() throws Exception {
        var list = List.of(IrsaliyeDTO.builder().id(1L).irsaliyeNo("IRS-001").durum("TASLAK").build());
        when(irsaliyeService.tumunuGetir(isNull(), any(Pageable.class))).thenReturn(new PageImpl<>(list));

        mockMvc.perform(get("/api/irsaliyeler"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].irsaliyeNo").value("IRS-001"));
    }

    @Test
    void shouldGetAllBySirket() throws Exception {
        var list = List.of(IrsaliyeDTO.builder().id(1L).irsaliyeNo("IRS-001").build());
        when(irsaliyeService.tumunuGetir(eq(1L), any(Pageable.class))).thenReturn(new PageImpl<>(list));

        mockMvc.perform(get("/api/irsaliyeler").requestAttr("sirketId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].irsaliyeNo").value("IRS-001"));
    }

    @Test
    void shouldGetById() throws Exception {
        var dto = IrsaliyeDTO.builder().id(1L).irsaliyeNo("IRS-001").build();
        when(irsaliyeService.getir(1L)).thenReturn(dto);

        mockMvc.perform(get("/api/irsaliyeler/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.irsaliyeNo").value("IRS-001"));
    }

    @Test
    void shouldReturnNotFoundWhenGetById() throws Exception {
        when(irsaliyeService.getir(anyLong())).thenThrow(new ResourceNotFoundException("İrsaliye bulunamadı: 999"));

        mockMvc.perform(get("/api/irsaliyeler/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldCreate() throws Exception {
        var dto = IrsaliyeDTO.builder().id(1L).irsaliyeNo("IRS-001").tarih(LocalDate.now()).build();
        when(irsaliyeService.olustur(any(IrsaliyeDTO.class), any())).thenReturn(dto);

        mockMvc.perform(post("/api/irsaliyeler")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.irsaliyeNo").value("IRS-001"));
    }

    @Test
    void shouldUpdateDurum() throws Exception {
        var dto = IrsaliyeDTO.builder().id(1L).durum("SEVKEDILDI").build();
        when(irsaliyeService.durumGuncelle(eq(1L), anyString())).thenReturn(dto);

        mockMvc.perform(put("/api/irsaliyeler/1/durum")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("durum", "SEVKEDILDI"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.durum").value("SEVKEDILDI"));
    }

    @Test
    void shouldDelete() throws Exception {
        doNothing().when(irsaliyeService).sil(1L);

        mockMvc.perform(delete("/api/irsaliyeler/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldTeslimEt() throws Exception {
        var dto = IrsaliyeDTO.builder().id(1L).durum("TESLIM_EDILDI").teslimAlan("Ahmet").build();
        when(irsaliyeService.teslim(eq(1L), any(com.raspel.erp.dto.muhasebe.IrsaliyeTeslimIstek.class), eq(7L)))
                .thenReturn(dto);

        mockMvc.perform(post("/api/irsaliyeler/1/teslim")
                        .requestAttr("kullaniciId", 7L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("teslimAlan", "Ahmet"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.durum").value("TESLIM_EDILDI"))
                .andExpect(jsonPath("$.teslimAlan").value("Ahmet"));
    }

    @Test
    void shouldSevkiGeriAl() throws Exception {
        var dto = IrsaliyeDTO.builder().id(1L).durum("HAZIRLANDI").build();
        when(irsaliyeService.sevkiGeriAl(1L, 7L)).thenReturn(dto);

        mockMvc.perform(post("/api/irsaliyeler/1/sevki-geri-al").requestAttr("kullaniciId", 7L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.durum").value("HAZIRLANDI"));
    }

    @Test
    void shouldIptalEt() throws Exception {
        var dto = IrsaliyeDTO.builder().id(1L).durum("IPTAL").build();
        when(irsaliyeService.iptal(eq(1L), eq("Müşteri vazgeçti"), eq(7L))).thenReturn(dto);

        mockMvc.perform(post("/api/irsaliyeler/1/iptal")
                        .requestAttr("kullaniciId", 7L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("aciklama", "Müşteri vazgeçti"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.durum").value("IPTAL"));
    }

    @Test
    void shouldTopluFatura() throws Exception {
        var sonuc = com.raspel.erp.dto.muhasebe.TopluFaturaSonucDTO.builder()
                .basarili(List.of(com.raspel.erp.dto.muhasebe.TopluFaturaSonucDTO.Basarili.builder()
                        .irsaliyeId(1L).irsaliyeNo("IRS-001").faturaId(10L).faturaNo("FTR-10").build()))
                .atlanan(List.of())
                .build();
        when(irsaliyeService.topluFatura(eq(List.of(1L)), eq(1L), eq(7L))).thenReturn(sonuc);

        mockMvc.perform(post("/api/irsaliyeler/toplu-fatura")
                        .requestAttr("sirketId", 1L)
                        .requestAttr("kullaniciId", 7L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("ids", List.of(1L)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.basarili[0].faturaNo").value("FTR-10"));
    }

    @Test
    void shouldGetFiltreli() throws Exception {
        var dto = IrsaliyeDTO.builder().id(1L).irsaliyeNo("IRS-001").durum("SEVK_EDILDI").build();
        when(irsaliyeService.filtreli(eq(1L), eq("IRS"), eq("SEVK_EDILDI"), eq("SATIS"), eq(5L),
                any(), any(), eq(Boolean.TRUE), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(dto)));

        mockMvc.perform(get("/api/irsaliyeler/filtreli")
                        .requestAttr("sirketId", 1L)
                        .param("q", "IRS")
                        .param("durum", "SEVK_EDILDI")
                        .param("tur", "SATIS")
                        .param("cariId", "5")
                        .param("faturali", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].irsaliyeNo").value("IRS-001"));
    }

    @Test
    void shouldGetOzet() throws Exception {
        when(irsaliyeService.ozet(eq(1L), any(), any()))
                .thenReturn(com.raspel.erp.dto.muhasebe.IrsaliyeOzetDTO.builder()
                        .taslak(2).hazirlanan(3).sevkEdilen(4).teslimEdilenBuAy(5).faturasiz(6).iptal(1)
                        .build());

        mockMvc.perform(get("/api/irsaliyeler/ozet").requestAttr("sirketId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.taslak").value(2))
                .andExpect(jsonPath("$.hazirlanan").value(3))
                .andExpect(jsonPath("$.faturasiz").value(6));
    }
}





