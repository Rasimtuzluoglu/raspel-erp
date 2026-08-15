package com.raspel.erp.controller.ik;

import com.raspel.erp.controller.TestSecurityMocks;
import com.raspel.erp.dto.ik.MaasBordroDTO;
import com.raspel.erp.service.ik.MaasBordroService;
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

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MaasBordroController.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@AutoConfigureMockMvc(addFilters = false)
@Import(TestSecurityMocks.class)
class MaasBordroControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private MaasBordroService maasBordroService;

    private MaasBordroDTO ornek() {
        return MaasBordroDTO.builder().id(1L).personelId(1L).personelAdi("Ahmet Yilmaz")
                .yil(2026).ay(7).brutMaas(new BigDecimal("30000.00"))
                .kesintiler(new BigDecimal("9000.00")).netMaas(new BigDecimal("21000.00"))
                .sirketId(1L).build();
    }

    @Test
    void shouldGetAll() throws Exception {
        when(maasBordroService.tumunuGetir(anyLong(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(ornek())));
        mockMvc.perform(get("/api/maas-bordro").requestAttr("sirketId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].personelAdi").value("Ahmet Yilmaz"));
    }

    @Test
    void shouldGetById() throws Exception {
        when(maasBordroService.getir(1L)).thenReturn(ornek());
        mockMvc.perform(get("/api/maas-bordro/1"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldCreate() throws Exception {
        when(maasBordroService.olustur(any(MaasBordroDTO.class), anyLong())).thenReturn(ornek());
        mockMvc.perform(post("/api/maas-bordro")
                        .requestAttr("sirketId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ornek())))
                .andExpect(status().isCreated());
    }

    @Test
    void shouldDelete() throws Exception {
        doNothing().when(maasBordroService).sil(1L);
        mockMvc.perform(delete("/api/maas-bordro/1"))
                .andExpect(status().isNoContent());
    }
}
