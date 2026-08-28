package com.raspel.erp.controller;

import com.raspel.erp.dto.finans.CekSenetDTO;
import com.raspel.erp.service.finans.CekSenetService;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import com.raspel.erp.exception.ResourceNotFoundException;
import org.springframework.test.annotation.DirtiesContext;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.raspel.erp.controller.finans.CekSenetController;

@WebMvcTest(CekSenetController.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@AutoConfigureMockMvc(addFilters = false)
@Import(TestSecurityMocks.class)
class CekSenetControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    
    @MockBean
    private CekSenetService cekSenetService;

    @Test
    void shouldGetAll() throws Exception {
        var list = List.of(CekSenetDTO.builder().id(1L).tur("CEK").cekNo("12345").durum("PORTFOY").build());
        when(cekSenetService.tumunuGetir(isNull(), any(Pageable.class))).thenReturn(new PageImpl<>(list));

        mockMvc.perform(get("/api/cek-senet"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].tur").value("CEK"));
    }

    @Test
    void shouldGetAllBySirket() throws Exception {
        var list = List.of(CekSenetDTO.builder().id(1L).tur("CEK").build());
        when(cekSenetService.tumunuGetir(eq(1L), any(Pageable.class))).thenReturn(new PageImpl<>(list));

        mockMvc.perform(get("/api/cek-senet").param("sirketId", "1").requestAttr("sirketId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].tur").value("CEK"));
    }

    @Test
    void shouldGetById() throws Exception {
        var dto = CekSenetDTO.builder().id(1L).tur("SENET").tutar(BigDecimal.valueOf(5000)).build();
        when(cekSenetService.getir(1L)).thenReturn(dto);

        mockMvc.perform(get("/api/cek-senet/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tur").value("SENET"));
    }

    @Test
    void shouldReturnNotFoundWhenGetById() throws Exception {
        when(cekSenetService.getir(anyLong())).thenThrow(new ResourceNotFoundException("Çek/Senet bulunamadı: 999"));

        mockMvc.perform(get("/api/cek-senet/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldCreate() throws Exception {
        var dto = CekSenetDTO.builder().id(1L).tur("CEK").cekNo("67890").vadeTarihi(LocalDate.now().plusMonths(3)).tutar(BigDecimal.valueOf(10000)).build();
        when(cekSenetService.olustur(any(CekSenetDTO.class))).thenReturn(dto);

        mockMvc.perform(post("/api/cek-senet")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tur").value("CEK"));
    }

    @Test
    void shouldUpdateDurum() throws Exception {
        var dto = CekSenetDTO.builder().id(1L).durum("TAHSIL_EDILDI").build();
        when(cekSenetService.durumGuncelle(eq(1L), anyString())).thenReturn(dto);

        mockMvc.perform(put("/api/cek-senet/1/durum")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("durum", "TAHSIL_EDILDI"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.durum").value("TAHSIL_EDILDI"));
    }

    @Test
    void shouldDelete() throws Exception {
        doNothing().when(cekSenetService).sil(1L);

        mockMvc.perform(delete("/api/cek-senet/1"))
                .andExpect(status().isNoContent());
    }
}





