package com.raspel.erp.controller.sistem;

import com.raspel.erp.controller.TestSecurityMocks;
import com.raspel.erp.dto.sistem.KokpitDTO;
import com.raspel.erp.service.sistem.DashboardService;
import com.raspel.erp.service.sistem.DashboardTercihService;
import com.raspel.erp.service.sistem.KokpitService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DashboardController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(TestSecurityMocks.class)
class DashboardControllerKokpitTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DashboardService dashboardService;

    @MockBean
    private DashboardTercihService dashboardTercihService;

    @MockBean
    private KokpitService kokpitService;

    @Test
    void kokpitDonemParametresiyleCagirilir() throws Exception {
        when(kokpitService.kokpitGetir(1L, "USER", "GECEN_AY"))
                .thenReturn(KokpitDTO.builder()
                        .donem(KokpitDTO.DonemDTO.builder().kod("GECEN_AY").etiket("Geçen Ay").build())
                        .ozet(List.of())
                        .kritikDurum(List.of())
                        .build());

        mockMvc.perform(get("/api/dashboard/kokpit")
                        .param("donem", "GECEN_AY")
                        .requestAttr("sirketId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.donem.kod").value("GECEN_AY"));

        verify(kokpitService).kokpitGetir(1L, "USER", "GECEN_AY");
    }

    @Test
    void kokpitVarsayilanDonemKullanir() throws Exception {
        when(kokpitService.kokpitGetir(1L, "USER", "BU_AY"))
                .thenReturn(KokpitDTO.builder()
                        .donem(KokpitDTO.DonemDTO.builder().kod("BU_AY").etiket("Bu Ay").build())
                        .ozet(List.of())
                        .kritikDurum(List.of())
                        .build());

        mockMvc.perform(get("/api/dashboard/kokpit")
                        .requestAttr("sirketId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.donem.kod").value("BU_AY"));

        verify(kokpitService).kokpitGetir(1L, "USER", "BU_AY");
    }

    @Test
    void kokpitGecersizDonemdeVarsayilanaDoner() throws Exception {
        when(kokpitService.kokpitGetir(1L, "USER", "BU_AY"))
                .thenReturn(KokpitDTO.builder()
                        .donem(KokpitDTO.DonemDTO.builder().kod("BU_AY").etiket("Bu Ay").build())
                        .ozet(List.of())
                        .kritikDurum(List.of())
                        .build());

        mockMvc.perform(get("/api/dashboard/kokpit")
                        .param("donem", "SAÇMA")
                        .requestAttr("sirketId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.donem.kod").value("BU_AY"));

        verify(kokpitService).kokpitGetir(1L, "USER", "BU_AY");
    }

    @Test
    void kokpitTenantBaglamiZorunlu() throws Exception {
        mockMvc.perform(get("/api/dashboard/kokpit"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Şirket bilgisi bulunamadı"));
    }
}
