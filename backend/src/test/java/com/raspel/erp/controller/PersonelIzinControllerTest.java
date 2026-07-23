package com.raspel.erp.controller;

import com.raspel.erp.dto.PersonelIzinDTO;
import com.raspel.erp.service.PersonelIzinService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.test.annotation.DirtiesContext;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PersonelIzinController.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@AutoConfigureMockMvc(addFilters = false)
@Import(TestSecurityMocks.class)
class PersonelIzinControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    
    @MockBean
    private PersonelIzinService personelIzinService;

    @Test
    void shouldGetAll() throws Exception {
        var list = List.of(PersonelIzinDTO.builder().id(1L).izinTuru("YILLIK_IZIN").durum("BEKLEMEDE").build());
        when(personelIzinService.tumunuGetir()).thenReturn(list);

        mockMvc.perform(get("/api/personel-izin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].izinTuru").value("YILLIK_IZIN"));
    }

    @Test
    void shouldGetByPersonelId() throws Exception {
        var list = List.of(PersonelIzinDTO.builder().id(1L).personelId(1L).build());
        when(personelIzinService.personelIzınleri(1L)).thenReturn(list);

        mockMvc.perform(get("/api/personel-izin/personel/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].personelId").value(1L));
    }

    @Test
    void shouldGetById() throws Exception {
        var dto = PersonelIzinDTO.builder().id(1L).izinTuru("HASTALIK").build();
        when(personelIzinService.getir(1L)).thenReturn(dto);

        mockMvc.perform(get("/api/personel-izin/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.izinTuru").value("HASTALIK"));
    }

    @Test
    void shouldReturnNotFoundWhenGetById() throws Exception {
        when(personelIzinService.getir(anyLong())).thenThrow(new RuntimeException("İzin kaydı bulunamadı: 999"));

        mockMvc.perform(get("/api/personel-izin/999"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldCreate() throws Exception {
        var dto = PersonelIzinDTO.builder().id(1L).personelId(1L).izinTuru("YILLIK_IZIN").baslangic(LocalDate.now()).bitis(LocalDate.now().plusDays(5)).gunSayisi(5).build();
        when(personelIzinService.olustur(any(PersonelIzinDTO.class))).thenReturn(dto);

        mockMvc.perform(post("/api/personel-izin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.izinTuru").value("YILLIK_IZIN"));
    }

    @Test
    void shouldUpdateDurum() throws Exception {
        var dto = PersonelIzinDTO.builder().id(1L).durum("ONAYLANDI").build();
        when(personelIzinService.durumGuncelle(eq(1L), anyString(), anyString())).thenReturn(dto);

        mockMvc.perform(put("/api/personel-izin/1/durum")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("durum", "ONAYLANDI", "onaylayan", "Yönetici"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.durum").value("ONAYLANDI"));
    }

    @Test
    void shouldDelete() throws Exception {
        doNothing().when(personelIzinService).sil(1L);

        mockMvc.perform(delete("/api/personel-izin/1"))
                .andExpect(status().isNoContent());
    }
}








