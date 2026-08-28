package com.raspel.erp.controller;

import com.raspel.erp.dto.sistem.GorevDTO;
import com.raspel.erp.dto.sistem.ProjeDTO;
import com.raspel.erp.service.sistem.ProjeService;
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
import com.raspel.erp.entity.sistem.Gorev;
import com.raspel.erp.entity.sistem.Proje;
import com.raspel.erp.controller.sistem.ProjeController;

@WebMvcTest(ProjeController.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@AutoConfigureMockMvc(addFilters = false)
@Import(TestSecurityMocks.class)
class ProjeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    
    @MockBean
    private ProjeService projeService;

    @Test
    void shouldGetAll() throws Exception {
        var list = List.of(ProjeDTO.builder().id(1L).ad("Proje A").durum("DEVAM_EDIYOR").build());
        when(projeService.tumunuGetir(isNull(), any(Pageable.class))).thenReturn(new PageImpl<>(list));

        mockMvc.perform(get("/api/projeler"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].ad").value("Proje A"));
    }

    @Test
    void shouldGetAllBySirket() throws Exception {
        var list = List.of(ProjeDTO.builder().id(1L).ad("Proje A").build());
        when(projeService.tumunuGetir(eq(1L), any(Pageable.class))).thenReturn(new PageImpl<>(list));

        mockMvc.perform(get("/api/projeler").param("sirketId", "1").requestAttr("sirketId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].ad").value("Proje A"));
    }

    @Test
    void shouldGetById() throws Exception {
        var dto = ProjeDTO.builder().id(1L).ad("Proje A").build();
        when(projeService.getir(1L)).thenReturn(dto);

        mockMvc.perform(get("/api/projeler/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ad").value("Proje A"));
    }

    @Test
    void shouldReturnNotFoundWhenGetById() throws Exception {
        when(projeService.getir(anyLong())).thenThrow(new ResourceNotFoundException("Proje bulunamadı: 999"));

        mockMvc.perform(get("/api/projeler/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldCreate() throws Exception {
        var dto = ProjeDTO.builder().id(1L).ad("Yeni Proje").baslangic(LocalDate.now()).build();
        when(projeService.olustur(any(ProjeDTO.class))).thenReturn(dto);

        mockMvc.perform(post("/api/projeler")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ad").value("Yeni Proje"));
    }

    @Test
    void shouldUpdateDurum() throws Exception {
        var dto = ProjeDTO.builder().id(1L).durum("TAMAMLANDI").build();
        when(projeService.durumGuncelle(eq(1L), anyString())).thenReturn(dto);

        mockMvc.perform(put("/api/projeler/1/durum")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("durum", "TAMAMLANDI"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.durum").value("TAMAMLANDI"));
    }

    @Test
    void shouldUpdateGorevDurum() throws Exception {
        var dto = GorevDTO.builder().id(1L).durum("TAMAMLANDI").build();
        when(projeService.gorevDurumGuncelle(eq(1L), anyString())).thenReturn(dto);

        mockMvc.perform(put("/api/projeler/gorev/1/durum")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("durum", "TAMAMLANDI"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.durum").value("TAMAMLANDI"));
    }

    @Test
    void shouldAddGorev() throws Exception {
        var gorevDto = GorevDTO.builder().ad("Alt Görev").build();
        var projeDto = ProjeDTO.builder().id(1L).ad("Proje A").build();
        when(projeService.gorevEkle(eq(1L), any(GorevDTO.class))).thenReturn(projeDto);

        mockMvc.perform(post("/api/projeler/1/gorevler")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(gorevDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ad").value("Proje A"));
    }

    @Test
    void shouldDelete() throws Exception {
        doNothing().when(projeService).sil(1L);

        mockMvc.perform(delete("/api/projeler/1"))
                .andExpect(status().isNoContent());
    }
}




