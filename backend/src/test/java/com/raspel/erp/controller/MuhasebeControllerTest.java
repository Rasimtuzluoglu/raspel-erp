package com.raspel.erp.controller;

import com.raspel.erp.controller.muhasebe.MuhasebeController;
import com.raspel.erp.dto.muhasebe.DefteriKebirSatiriDTO;
import com.raspel.erp.dto.muhasebe.HesapPlaniDTO;
import com.raspel.erp.dto.muhasebe.MizanSatiriDTO;
import com.raspel.erp.dto.muhasebe.MuhasebeFisKalemDTO;
import com.raspel.erp.dto.muhasebe.MuhasebeFisiDTO;
import com.raspel.erp.service.muhasebe.MuhasebeService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MuhasebeController.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@AutoConfigureMockMvc(addFilters = false)
@Import(TestSecurityMocks.class)
class MuhasebeControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private MuhasebeService muhasebeService;

    private HesapPlaniDTO hesapOrnek() {
        return HesapPlaniDTO.builder().id(1L).kod("100").ad("Kasa").tip("AKTIF").sirketId(1L).aktif(true).build();
    }

    private MuhasebeFisiDTO fisOrnek() {
        return MuhasebeFisiDTO.builder().id(1L).fisNo("MUH-2024-000001").tarih(LocalDate.now())
                .aciklama("Test Fisi").durum("KAYITLI").sirketId(1L)
                .toplamBorc(new BigDecimal("100.00")).toplamAlacak(new BigDecimal("100.00"))
                .kalemler(List.of(MuhasebeFisKalemDTO.builder().id(1L).hesapKodu("100")
                        .borc(new BigDecimal("100.00")).alacak(BigDecimal.ZERO).build()))
                .build();
    }

    @Test
    void shouldGetHesapPlani() throws Exception {
        when(muhasebeService.hesapPlaniniGetir(anyLong())).thenReturn(List.of(hesapOrnek()));
        mockMvc.perform(get("/api/muhasebe/hesap-plani").requestAttr("sirketId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].kod").value("100"));
    }

    @Test
    void shouldCreateHesap() throws Exception {
        when(muhasebeService.hesapOlustur(any(HesapPlaniDTO.class))).thenReturn(hesapOrnek());
        mockMvc.perform(post("/api/muhasebe/hesap-plani")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(hesapOrnek())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ad").value("Kasa"));
    }

    @Test
    void shouldGetFisler() throws Exception {
        when(muhasebeService.fisleriGetir(anyLong(), isNull(), isNull())).thenReturn(List.of(fisOrnek()));
        mockMvc.perform(get("/api/muhasebe/fisler").requestAttr("sirketId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].fisNo").value("MUH-2024-000001"));
    }

    @Test
    void shouldGetMizan() throws Exception {
        when(muhasebeService.mizanGetir(anyLong(), isNull(), isNull())).thenReturn(
                List.of(MizanSatiriDTO.builder().hesapKodu("100").hesapAdi("Kasa")
                        .borc(new BigDecimal("100.00")).alacak(BigDecimal.ZERO)
                        .borcBakiye(new BigDecimal("100.00")).alacakBakiye(BigDecimal.ZERO).build()));
        mockMvc.perform(get("/api/muhasebe/mizan").requestAttr("sirketId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].hesapKodu").value("100"));
    }

    @Test
    void shouldGetDefteriKebir() throws Exception {
        when(muhasebeService.defteriKebirGetir(anyLong(), isNull(), isNull(), isNull())).thenReturn(
                List.of(DefteriKebirSatiriDTO.builder().tarih(LocalDate.now()).fisNo("MUH-2024-000001")
                        .borc(new BigDecimal("100.00")).alacak(BigDecimal.ZERO)
                        .bakiye(new BigDecimal("100.00")).build()));
        mockMvc.perform(get("/api/muhasebe/defteri-kebir").requestAttr("sirketId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].fisNo").value("MUH-2024-000001"));
    }
}
