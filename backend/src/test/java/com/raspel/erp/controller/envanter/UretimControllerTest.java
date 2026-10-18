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
        when(uretimService.filtreliEmirler(any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(UretimEmriDTO.builder()
                        .id(1L).urunAd("Masa").durum("TASLAK").oncelik("YUKSEK").build())));

        mockMvc.perform(get("/api/uretim/emirler/filtreli")
                        .requestAttr("sirketId", 1L)
                        .param("durum", "TASLAK")
                        .param("geciken", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].urunAd").value("Masa"))
                .andExpect(jsonPath("$.totalElements").value(1));

        verify(uretimService).filtreliEmirler(eq(1L), any(), eq("TASLAK"), any(), eq(true), any(),
                any(), any(), any());
    }
}
