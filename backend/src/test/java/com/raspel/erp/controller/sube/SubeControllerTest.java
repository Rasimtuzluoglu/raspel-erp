package com.raspel.erp.controller.sube;

import com.raspel.erp.controller.TestSecurityMocks;
import com.raspel.erp.dto.sube.SubeDTO;
import com.raspel.erp.service.sube.SubeService;
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

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SubeController.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@AutoConfigureMockMvc(addFilters = false)
@Import(TestSecurityMocks.class)
class SubeControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private SubeService subeService;

    private SubeDTO ornek() {
        return SubeDTO.builder().id(1L).ad("Merkez Şube").telefon("0212 000 00 00").aktif(true).build();
    }

    @Test
    void shouldGetAll() throws Exception {
        when(subeService.tumunuGetir(anyLong(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(ornek())));
        mockMvc.perform(get("/api/subeler").requestAttr("sirketId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].ad").value("Merkez Şube"));
    }

    @Test
    void shouldGetAktif() throws Exception {
        when(subeService.aktifSubeler(1L)).thenReturn(List.of(ornek()));
        mockMvc.perform(get("/api/subeler/aktif").requestAttr("sirketId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].ad").value("Merkez Şube"));
    }

    @Test
    void shouldGetById() throws Exception {
        when(subeService.getir(1L)).thenReturn(ornek());
        mockMvc.perform(get("/api/subeler/1"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldCreate() throws Exception {
        when(subeService.olustur(any(SubeDTO.class))).thenReturn(ornek());
        mockMvc.perform(post("/api/subeler")
                        .requestAttr("sirketId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ornek())))
                .andExpect(status().isCreated());
    }

    @Test
    void shouldDelete() throws Exception {
        doNothing().when(subeService).sil(1L);
        mockMvc.perform(delete("/api/subeler/1"))
                .andExpect(status().isNoContent());
    }
}
