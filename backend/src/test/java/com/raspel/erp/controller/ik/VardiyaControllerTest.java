package com.raspel.erp.controller.ik;

import com.raspel.erp.controller.TestSecurityMocks;
import com.raspel.erp.dto.ik.VardiyaDTO;
import com.raspel.erp.service.ik.VardiyaService;
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

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(VardiyaController.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@AutoConfigureMockMvc(addFilters = false)
@Import(TestSecurityMocks.class)
class VardiyaControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private VardiyaService vardiyaService;

    private VardiyaDTO ornek() {
        return VardiyaDTO.builder().id(1L).personelId(1L).personelAdi("Ahmet Yilmaz")
                .tarih(LocalDate.of(2026, 8, 15))
                .baslangic(LocalTime.of(8, 0)).bitis(LocalTime.of(17, 0))
                .tur("SABAH").sirketId(1L).build();
    }

    @Test
    void shouldGetAll() throws Exception {
        when(vardiyaService.tumunuGetir(anyLong(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(ornek())));
        mockMvc.perform(get("/api/vardiyalar").requestAttr("sirketId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].tur").value("SABAH"));
    }

    @Test
    void shouldGetById() throws Exception {
        when(vardiyaService.getir(1L)).thenReturn(ornek());
        mockMvc.perform(get("/api/vardiyalar/1"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldGetByPersonel() throws Exception {
        when(vardiyaService.personelVardiyalari(1L)).thenReturn(List.of(ornek()));
        mockMvc.perform(get("/api/vardiyalar/personel/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].personelId").value(1L));
    }

    @Test
    void shouldCreate() throws Exception {
        when(vardiyaService.olustur(any(VardiyaDTO.class), anyLong())).thenReturn(ornek());
        mockMvc.perform(post("/api/vardiyalar")
                        .requestAttr("sirketId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ornek())))
                .andExpect(status().isCreated());
    }

    @Test
    void shouldDelete() throws Exception {
        doNothing().when(vardiyaService).sil(1L);
        mockMvc.perform(delete("/api/vardiyalar/1"))
                .andExpect(status().isNoContent());
    }
}
