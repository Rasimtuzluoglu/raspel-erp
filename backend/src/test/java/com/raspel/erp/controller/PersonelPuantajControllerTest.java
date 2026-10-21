package com.raspel.erp.controller;

import com.raspel.erp.dto.ik.PersonelPuantajDTO;
import com.raspel.erp.service.ik.PersonelPuantajService;
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
import org.springframework.test.annotation.DirtiesContext;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.raspel.erp.entity.ik.Personel;
import com.raspel.erp.controller.ik.PersonelPuantajController;

@WebMvcTest(PersonelPuantajController.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@AutoConfigureMockMvc(addFilters = false)
@Import(TestSecurityMocks.class)
class PersonelPuantajControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    
    @MockBean
    private PersonelPuantajService personelPuantajService;

    @Test
    void shouldGetByPersonel() throws Exception {
        var list = List.of(PersonelPuantajDTO.builder().id(1L).personelId(1L).tarih(LocalDate.now()).durum("GELDI").build());
        when(personelPuantajService.personelPuantajlari(eq(1L), any(LocalDate.class), any(LocalDate.class))).thenReturn(list);

        mockMvc.perform(get("/api/personel-puantaj/personel/1")
                        .param("baslangic", "2024-01-01")
                        .param("bitis", "2024-01-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].durum").value("GELDI"));
    }

    @Test
    void shouldCreate() throws Exception {
        var dto = PersonelPuantajDTO.builder().id(1L).personelId(1L).tarih(LocalDate.now()).durum("GELDI").build();
        when(personelPuantajService.olustur(any(PersonelPuantajDTO.class))).thenReturn(dto);

        mockMvc.perform(post("/api/personel-puantaj")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.durum").value("GELDI"));
    }

    @Test
    void shouldGetFiltreli() throws Exception {
        var list = List.of(PersonelPuantajDTO.builder().id(1L).personelId(3L).durum("GELDI").build());
        when(personelPuantajService.filtreli(eq(1L), eq(3L), any(), any(), eq("GELDI"), eq("ahmet"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(list));

        mockMvc.perform(get("/api/personel-puantaj/filtreli")
                        .requestAttr("sirketId", 1L)
                        .param("personelId", "3")
                        .param("baslangic", "2026-10-01")
                        .param("bitis", "2026-10-31")
                        .param("durum", "GELDI")
                        .param("q", "ahmet"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].durum").value("GELDI"));

        verify(personelPuantajService).filtreli(eq(1L), eq(3L),
                any(java.time.LocalDate.class), any(java.time.LocalDate.class), eq("GELDI"), eq("ahmet"), any(Pageable.class));
    }

    @Test
    void shouldGetOzet() throws Exception {
        when(personelPuantajService.ozet(1L)).thenReturn(com.raspel.erp.dto.ik.PuantajOzetDTO.builder()
                .toplam(20).geldi(12).gelmedi(3).izinli(4).mazeretli(1).build());

        mockMvc.perform(get("/api/personel-puantaj/ozet").requestAttr("sirketId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.toplam").value(20))
                .andExpect(jsonPath("$.geldi").value(12))
                .andExpect(jsonPath("$.gelmedi").value(3))
                .andExpect(jsonPath("$.mazeretli").value(1));
    }

    @Test
    void shouldDelete() throws Exception {
        doNothing().when(personelPuantajService).sil(1L);

        mockMvc.perform(delete("/api/personel-puantaj/1"))
                .andExpect(status().isNoContent());
    }
}




