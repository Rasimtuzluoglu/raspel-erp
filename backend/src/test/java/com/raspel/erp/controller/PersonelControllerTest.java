package com.raspel.erp.controller;

import com.raspel.erp.dto.ik.PersonelDTO;
import com.raspel.erp.service.ik.PersonelService;
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
import com.raspel.erp.exception.ResourceNotFoundException;
import org.springframework.test.annotation.DirtiesContext;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.raspel.erp.entity.ik.Personel;
import com.raspel.erp.controller.ik.PersonelController;

@WebMvcTest(PersonelController.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@AutoConfigureMockMvc(addFilters = false)
@Import(TestSecurityMocks.class)
class PersonelControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    
    @MockBean
    private PersonelService personelService;

    @Test
    void shouldGetAll() throws Exception {
        var list = List.of(PersonelDTO.builder().id(1L).ad("Ahmet").soyad("Yılmaz").build());
        when(personelService.tumunuGetir(isNull(), any(Pageable.class))).thenReturn(new PageImpl<>(list));

        mockMvc.perform(get("/api/personel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].ad").value("Ahmet"));
    }

    @Test
    void shouldGetAllBySirket() throws Exception {
        var list = List.of(PersonelDTO.builder().id(1L).ad("Ahmet").build());
        when(personelService.tumunuGetir(eq(1L), any(Pageable.class))).thenReturn(new PageImpl<>(list));

        mockMvc.perform(get("/api/personel").param("sirketId", "1").requestAttr("sirketId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].ad").value("Ahmet"));
        verify(personelService).tumunuGetir(eq(1L), any(Pageable.class));
    }

    @Test
    void shouldIgnoreSirketIdQueryParam() throws Exception {
        var list = List.of(PersonelDTO.builder().id(1L).ad("Ahmet").build());
        when(personelService.tumunuGetir(eq(7L), any(Pageable.class))).thenReturn(new PageImpl<>(list));

        mockMvc.perform(get("/api/personel").param("sirketId", "999").requestAttr("sirketId", 7L))
                .andExpect(status().isOk());
        verify(personelService).tumunuGetir(eq(7L), any(Pageable.class));
        verify(personelService, never()).tumunuGetir(eq(999L), any(Pageable.class));
    }

    @Test
    void shouldGetById() throws Exception {
        var dto = PersonelDTO.builder().id(1L).ad("Ahmet").soyad("Yılmaz").build();
        when(personelService.getir(1L)).thenReturn(dto);

        mockMvc.perform(get("/api/personel/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.soyad").value("Yılmaz"));
    }

    @Test
    void shouldReturnNotFoundWhenGetById() throws Exception {
        when(personelService.getir(anyLong())).thenThrow(new ResourceNotFoundException("Personel bulunamadı: 999"));

        mockMvc.perform(get("/api/personel/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldCreate() throws Exception {
        var dto = PersonelDTO.builder().id(1L).ad("Mehmet").soyad("Kaya").iseGirisTarihi(LocalDate.now()).build();
        when(personelService.olustur(any(PersonelDTO.class))).thenReturn(dto);

        mockMvc.perform(post("/api/personel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ad").value("Mehmet"));
    }

    @Test
    void shouldUpdate() throws Exception {
        var dto = PersonelDTO.builder().id(1L).ad("Güncel").soyad("Personel").build();
        when(personelService.guncelle(eq(1L), any(PersonelDTO.class))).thenReturn(dto);

        mockMvc.perform(put("/api/personel/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ad").value("Güncel"));
    }

    @Test
    void shouldDelete() throws Exception {
        doNothing().when(personelService).sil(1L);

        mockMvc.perform(delete("/api/personel/1"))
                .andExpect(status().isNoContent());
    }
}




