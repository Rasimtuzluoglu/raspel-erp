package com.raspel.erp.controller.sistem;

import com.raspel.erp.controller.TestSecurityMocks;
import com.raspel.erp.dto.sistem.BildirimDTO;
import com.raspel.erp.service.sistem.BildirimService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BildirimController.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@AutoConfigureMockMvc(addFilters = false)
@Import(TestSecurityMocks.class)
class BildirimControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private BildirimService bildirimService;

    @Test
    void liste_donar() throws Exception {
        when(bildirimService.liste(null)).thenReturn(List.of(
                BildirimDTO.builder().id(1L).baslik("Bildirim").okundu(false).build()));

        mockMvc.perform(get("/api/bildirimler"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].baslik").value("Bildirim"));
    }

    @Test
    void okunmamis_sayisiDonar() throws Exception {
        when(bildirimService.okunmamisSayisi(null)).thenReturn(5L);

        mockMvc.perform(get("/api/bildirimler/okunmamis"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.adet").value(5));
    }

    @Test
    void okundu_isaretler() throws Exception {
        mockMvc.perform(put("/api/bildirimler/1/okundu"))
                .andExpect(status().isOk());
        verify(bildirimService).okunduIsaretle(1L);
    }

    @Test
    void tumuOkundu_isaretler() throws Exception {
        mockMvc.perform(put("/api/bildirimler/tumu-okundu"))
                .andExpect(status().isOk());
        verify(bildirimService).tumunuOkunduIsaretle(null);
    }
}
