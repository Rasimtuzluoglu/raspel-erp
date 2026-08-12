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
}





