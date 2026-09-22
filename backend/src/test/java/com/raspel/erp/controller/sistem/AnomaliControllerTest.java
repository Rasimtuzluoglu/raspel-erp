package com.raspel.erp.controller.sistem;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.raspel.erp.controller.TestSecurityMocks;
import com.raspel.erp.dto.sistem.AnomaliDTO;
import com.raspel.erp.service.sistem.AnomaliTespitEngine;
import com.raspel.erp.service.sistem.IpWhitelistService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AnomaliController.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@AutoConfigureMockMvc(addFilters = false)
@Import(TestSecurityMocks.class)
class AnomaliControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private AnomaliTespitEngine anomaliTespitEngine;
    @MockBean private IpWhitelistService ipWhitelistService;

    @Test
    void shouldGetAnomaliler() throws Exception {
        var list = List.of(AnomaliDTO.builder().id("1").tur("MUKERRER_FATURA").baslik("Mükerrer").build());
        when(anomaliTespitEngine.anomalileriTara(1L)).thenReturn(list);

        mockMvc.perform(get("/api/anomaliler").requestAttr("sirketId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tur").value("MUKERRER_FATURA"));
    }

    @Test
    void shouldGetIpWhitelist() throws Exception {
        Map<String, Object> map = new java.util.HashMap<>();
        map.put("id", "1");
        map.put("ipAdresi", "192.168.1.1");
        map.put("aciklama", "Ofis");
        when(ipWhitelistService.listele(1L)).thenReturn(List.of(map));

        mockMvc.perform(get("/api/anomaliler/ip-whitelist").requestAttr("sirketId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].ipAdresi").value("192.168.1.1"));
    }

    @Test
    void shouldAddIpWhitelist() throws Exception {
        Map<String, Object> entry = new java.util.HashMap<>();
        entry.put("ipAdresi", "10.0.0.1");
        entry.put("aciklama", "VPN");
        when(ipWhitelistService.ekle(eq(1L), any())).thenReturn(List.of(entry));

        mockMvc.perform(post("/api/anomaliler/ip-whitelist")
                        .requestAttr("sirketId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(entry)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].ipAdresi").value("10.0.0.1"));
    }

    @Test
    void shouldDeleteIpWhitelist() throws Exception {
        when(ipWhitelistService.sil(eq(1L), eq(1L))).thenReturn(List.of());

        mockMvc.perform(delete("/api/anomaliler/ip-whitelist/1").requestAttr("sirketId", 1L))
                .andExpect(status().isOk());
    }
}
