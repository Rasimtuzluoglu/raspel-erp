package com.raspel.erp.controller.envanter;

import com.raspel.erp.controller.TestSecurityMocks;
import com.raspel.erp.dto.envanter.UretimEmriDTO;
import com.raspel.erp.service.envanter.UretimService;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UretimController.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@AutoConfigureMockMvc(addFilters = false)
@Import(TestSecurityMocks.class)
class UretimControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UretimService uretimService;

    @Test
    void filtreliEmirler_sayfaliDonerVeParametreleriIletir() throws Exception {
        when(uretimService.filtreliEmirler(any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(UretimEmriDTO.builder()
                        .id(1L).urunAd("Masa").durum("TASLAK").oncelik("YUKSEK").build())));

        mockMvc.perform(get("/api/uretim/emirler/filtreli")
                        .requestAttr("sirketId", 1L)
                        .param("durum", "TASLAK")
                        .param("geciken", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].urunAd").value("Masa"))
                .andExpect(jsonPath("$.totalElements").value(1));

        verify(uretimService).filtreliEmirler(eq(1L), any(), eq("TASLAK"), any(), any(), eq(true), any(),
                any(), any(), any());
    }

    @Test
    void hatAta_hatIdIleServiseGider() throws Exception {
        when(uretimService.emirHatAta(4L, 5L, 1L, 7L)).thenReturn(UretimEmriDTO.builder()
                .id(4L).urunAd("Masa").hatId(5L).hatAd("Hat A").build());

        mockMvc.perform(put("/api/uretim/emirler/4/hat")
                        .requestAttr("sirketId", 1L)
                        .requestAttr("kullaniciId", 7L)
                        .param("hatId", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hatId").value(5))
                .andExpect(jsonPath("$.hatAd").value("Hat A"));
    }

    @Test
    void analiz_donemOzetiDoner() throws Exception {
        when(uretimService.analiz(1L, null, null)).thenReturn(
                com.raspel.erp.dto.envanter.UretimAnalizDTO.builder()
                        .tamamlananEmir(2)
                        .toplamUretilen(new java.math.BigDecimal("14.00"))
                        .fireOrani(new java.math.BigDecimal("6.67"))
                        .toplamMaliyet(new java.math.BigDecimal("280.00"))
                        .build());

        mockMvc.perform(get("/api/uretim/analiz").requestAttr("sirketId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tamamlananEmir").value(2))
                .andExpect(jsonPath("$.fireOrani").value(6.67));
    }

    @Test
    void takvim_planlananEmirleriDoner() throws Exception {
        when(uretimService.takvim(1L, null, null)).thenReturn(List.of(UretimEmriDTO.builder()
                .id(1L).urunAd("Masa").durum("URETIMDE").hatAd("Hat A").build()));

        mockMvc.perform(get("/api/uretim/takvim").requestAttr("sirketId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].urunAd").value("Masa"))
                .andExpect(jsonPath("$[0].hatAd").value("Hat A"));
    }
}
