package com.raspel.erp.controller.envanter;

import com.raspel.erp.controller.TestSecurityMocks;
import com.raspel.erp.dto.envanter.StokSeriDTO;
import com.raspel.erp.service.envanter.StokSeriService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StokSeriController.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@AutoConfigureMockMvc(addFilters = false)
@Import(TestSecurityMocks.class)
class StokSeriControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StokSeriService stokSeriService;

    @Test
    void tumu_donar() throws Exception {
        StokSeriDTO dto = StokSeriDTO.builder().id(1L).seriNo("SN-1").build();
        when(stokSeriService.tumunuGetir(any(), any())).thenReturn(new PageImpl<>(List.of(dto)));

        mockMvc.perform(get("/api/stok-seri"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].seriNo").value("SN-1"));
    }

    @Test
    void stokIcinGetir_donar() throws Exception {
        when(stokSeriService.stokIcinGetir(10L)).thenReturn(List.of(
                StokSeriDTO.builder().id(1L).seriNo("SN-1").build()));

        mockMvc.perform(get("/api/stok-seri/stok/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].seriNo").value("SN-1"));
    }

    @Test
    void getir_donar() throws Exception {
        when(stokSeriService.getir(1L)).thenReturn(StokSeriDTO.builder().id(1L).seriNo("SN-1").build());

        mockMvc.perform(get("/api/stok-seri/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void filtreli_donar() throws Exception {
        when(stokSeriService.filtreliGetir(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(
                        StokSeriDTO.builder().id(1L).seriNo("SN-1").durum("STOKTA").build())));

        mockMvc.perform(get("/api/stok-seri/filtreli")
                        .requestAttr("sirketId", 1L)
                        .param("durum", "STOKTA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].seriNo").value("SN-1"));
    }

    @Test
    void ozet_donar() throws Exception {
        when(stokSeriService.ozet(1L, 30))
                .thenReturn(com.raspel.erp.dto.envanter.StokSeriOzetDTO.builder().toplam(3).build());

        mockMvc.perform(get("/api/stok-seri/ozet").requestAttr("sirketId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.toplam").value(3));
    }
}
