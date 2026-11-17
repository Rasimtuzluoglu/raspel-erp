package com.raspel.erp.controller.sistem;

import com.raspel.erp.controller.TestSecurityMocks;
import com.raspel.erp.dto.sistem.SahaOzetDTO;
import com.raspel.erp.service.sistem.SahaOzetService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SahaController.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@AutoConfigureMockMvc(addFilters = false)
@Import(TestSecurityMocks.class)
class SahaControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private SahaOzetService sahaOzetService;

    @Test
    void ozet_donar() throws Exception {
        when(sahaOzetService.ozet(eq(1L), eq(9L))).thenReturn(SahaOzetDTO.builder()
                .teslimat(SahaOzetDTO.Teslimat.builder()
                        .bekleyen(3).yolda(1).bugunTeslimEdilen(2).geciken(1).build())
                .ziyaret(SahaOzetDTO.Ziyaret.builder().bugun(2).build())
                .talepler(SahaOzetDTO.Talepler.builder().bekleyenIzin(1).bekleyenMasraf(2).build())
                .build());

        mockMvc.perform(get("/api/saha/ozet")
                        .requestAttr("sirketId", 1L)
                        .requestAttr("kullaniciId", 9L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.teslimat.bekleyen").value(3))
                .andExpect(jsonPath("$.teslimat.geciken").value(1))
                .andExpect(jsonPath("$.ziyaret.bugun").value(2))
                .andExpect(jsonPath("$.talepler.bekleyenMasraf").value(2));
    }

    @Test
    void ozet_kullaniciBaglaminiGecirir() throws Exception {
        when(sahaOzetService.ozet(eq(2L), eq(7L))).thenReturn(SahaOzetDTO.builder().build());

        mockMvc.perform(get("/api/saha/ozet")
                        .requestAttr("sirketId", 2L)
                        .requestAttr("kullaniciId", 7L))
                .andExpect(status().isOk());
    }
}
