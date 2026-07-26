package com.raspel.erp.controller;

import com.raspel.erp.dto.DashboardDTO;
import com.raspel.erp.dto.HareketDTO;
import com.raspel.erp.service.DashboardService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.test.annotation.DirtiesContext;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DashboardController.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@AutoConfigureMockMvc(addFilters = false)
@Import(TestSecurityMocks.class)
class DashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    
    @MockBean
    private DashboardService dashboardService;

    @Test
    void shouldGetDashboard() throws Exception {
        var dto = DashboardDTO.builder()
                .toplamCariSayisi(10L)
                .toplamBakiye(BigDecimal.valueOf(100000))
                .sonHareketler(List.of(HareketDTO.builder().id(1L).tur("TAHSILAT").build()))
                .build();
        when(dashboardService.dashboardVerileriGetir(any())).thenReturn(dto);

        mockMvc.perform(get("/api/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.toplamCariSayisi").value(10L))
                .andExpect(jsonPath("$.toplamBakiye").value(100000));
    }
}








