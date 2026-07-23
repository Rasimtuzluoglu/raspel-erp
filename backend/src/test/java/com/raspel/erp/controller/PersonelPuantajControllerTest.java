package com.raspel.erp.controller;

import com.raspel.erp.dto.PersonelPuantajDTO;
import com.raspel.erp.service.PersonelPuantajService;
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
import org.springframework.test.annotation.DirtiesContext;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

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
    void shouldDelete() throws Exception {
        doNothing().when(personelPuantajService).sil(1L);

        mockMvc.perform(delete("/api/personel-puantaj/1"))
                .andExpect(status().isNoContent());
    }
}








