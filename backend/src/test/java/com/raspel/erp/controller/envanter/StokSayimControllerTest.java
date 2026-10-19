package com.raspel.erp.controller.envanter;

import com.raspel.erp.controller.TestSecurityMocks;
import com.raspel.erp.dto.envanter.StokSayimDTO;
import com.raspel.erp.dto.envanter.StokSayimOzetDTO;
import com.raspel.erp.service.envanter.StokSayimService;
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

@WebMvcTest(StokSayimController.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@AutoConfigureMockMvc(addFilters = false)
@Import(TestSecurityMocks.class)
class StokSayimControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StokSayimService stokSayimService;

    @Test
    void filtreli_sayfaliDonerVeParametreleriIletir() throws Exception {
        when(stokSayimService.filtreliGetir(any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(StokSayimDTO.builder()
                        .id(1L).stokAdi("Masa").durum("TASLAK").build())));

        mockMvc.perform(get("/api/stok-sayim/filtreli")
                        .requestAttr("sirketId", 1L)
                        .param("q", "ma")
                        .param("durum", "TASLAK"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].stokAdi").value("Masa"))
                .andExpect(jsonPath("$.totalElements").value(1));

        verify(stokSayimService).filtreliGetir(eq(1L), eq("ma"), eq("TASLAK"), any(), any(), any());
    }

    @Test
    void ozet_donar() throws Exception {
        when(stokSayimService.ozet(1L)).thenReturn(StokSayimOzetDTO.builder().toplam(2).build());

        mockMvc.perform(get("/api/stok-sayim/ozet").requestAttr("sirketId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.toplam").value(2));
    }
}
