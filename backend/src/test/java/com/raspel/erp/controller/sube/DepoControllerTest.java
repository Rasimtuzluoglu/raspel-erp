package com.raspel.erp.controller.sube;

import com.raspel.erp.controller.TestSecurityMocks;
import com.raspel.erp.dto.sube.DepoDTO;
import com.raspel.erp.service.sube.DepoService;
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
import com.raspel.erp.entity.sube.Depo;

@WebMvcTest(DepoController.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@AutoConfigureMockMvc(addFilters = false)
@Import(TestSecurityMocks.class)
class DepoControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private DepoService depoService;

    private DepoDTO ornek() {
        return DepoDTO.builder().id(1L).ad("Ana Depo").subeId(1L).aktif(true).build();
    }

    @Test
    void shouldGetAll() throws Exception {
        when(depoService.tumunuGetir(anyLong(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(ornek())));
        mockMvc.perform(get("/api/depolar").requestAttr("sirketId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].ad").value("Ana Depo"));
    }

    @Test
    void shouldGetById() throws Exception {
        when(depoService.getir(1L)).thenReturn(ornek());
        mockMvc.perform(get("/api/depolar/1"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldCreate() throws Exception {
        when(depoService.olustur(any(DepoDTO.class))).thenReturn(ornek());
        mockMvc.perform(post("/api/depolar")
                        .requestAttr("sirketId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ornek())))
                .andExpect(status().isCreated());
    }

    @Test
    void shouldDelete() throws Exception {
        doNothing().when(depoService).sil(1L);
        mockMvc.perform(delete("/api/depolar/1"))
                .andExpect(status().isNoContent());
    }
}