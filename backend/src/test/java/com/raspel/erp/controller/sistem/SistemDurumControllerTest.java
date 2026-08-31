package com.raspel.erp.controller.sistem;

import com.raspel.erp.controller.TestSecurityMocks;
import com.raspel.erp.dto.sistem.HataLogDTO;
import com.raspel.erp.service.sistem.SistemDurumService;
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
import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SistemDurumController.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@AutoConfigureMockMvc(addFilters = false)
@Import(TestSecurityMocks.class)
class SistemDurumControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SistemDurumService sistemDurumService;

    @Test
    void durum_donar() throws Exception {
        when(sistemDurumService.durum()).thenReturn(Map.of("durum", "UP"));

        mockMvc.perform(get("/api/sistem/durum"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.durum").value("UP"));
    }

    @Test
    void hataLog_listeler() throws Exception {
        when(sistemDurumService.sonHatalar(50)).thenReturn(List.of(
                HataLogDTO.builder().id(1L).tur("500").mesaj("Hata").endpoint("/api/x").build()));

        mockMvc.perform(get("/api/sistem/hata-log"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tur").value("500"))
                .andExpect(jsonPath("$[0].mesaj").value("Hata"));
    }
}
