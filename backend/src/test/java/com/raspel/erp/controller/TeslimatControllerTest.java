package com.raspel.erp.controller;

import com.raspel.erp.controller.ticaret.TeslimatController;
import com.raspel.erp.dto.ticaret.TeslimatDTO;
import com.raspel.erp.service.ticaret.TeslimatService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TeslimatController.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@AutoConfigureMockMvc(addFilters = false)
@Import(TestSecurityMocks.class)
class TeslimatControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private TeslimatService teslimatService;

    @Test
    void shouldListSuruculer() throws Exception {
        when(teslimatService.suruculer(1L, 2L)).thenReturn(List.of());

        mockMvc.perform(get("/api/drivers")
                        .requestAttr("sirketId", 1L)
                        .requestAttr("kullaniciId", 2L))
                .andExpect(status().isOk());
    }

    @Test
    void shouldListTeslimatlar() throws Exception {
        var dto = TeslimatDTO.builder().id(1L).build();
        when(teslimatService.teslimatlar(1L, 7L, 2L)).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/deliveries")
                        .param("driverId", "7")
                        .requestAttr("sirketId", 1L)
                        .requestAttr("kullaniciId", 2L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    void shouldGetTeslimatById() throws Exception {
        when(teslimatService.getir(5L, 1L, 2L)).thenReturn(TeslimatDTO.builder().id(5L).build());

        mockMvc.perform(get("/api/deliveries/5")
                        .requestAttr("sirketId", 1L)
                        .requestAttr("kullaniciId", 2L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5));
    }
}
