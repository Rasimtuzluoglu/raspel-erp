package com.raspel.erp.controller.ticaret;

import com.raspel.erp.controller.TestSecurityMocks;
import com.raspel.erp.dto.ticaret.CariFirsatDTO;
import com.raspel.erp.service.ticaret.CrmService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CrmController.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@AutoConfigureMockMvc(addFilters = false)
@Import(TestSecurityMocks.class)
class CrmControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CrmService crmService;

    @Test
    void firsatlar_donar() throws Exception {
        when(crmService.firsatlariGetir(any(), any())).thenReturn(List.of(
                CariFirsatDTO.builder().id(1L).ad("Fırsat").durum("YENI").build()));

        mockMvc.perform(get("/api/crm/firsatlar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].ad").value("Fırsat"));
    }

    @Test
    void firsatOlustur_yaratir() throws Exception {
        CariFirsatDTO dto = CariFirsatDTO.builder().ad("Yeni").durum("YENI").deger(new BigDecimal("100")).build();
        when(crmService.firsatOlustur(any(CariFirsatDTO.class), any())).thenReturn(CariFirsatDTO.builder().id(1L).ad("Yeni").build());

        mockMvc.perform(post("/api/crm/firsatlar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ad").value("Yeni"));
    }

    @Test
    void firsatSil_siler() throws Exception {
        mockMvc.perform(delete("/api/crm/firsatlar/1"))
                .andExpect(status().isNoContent());
        verify(crmService).firsatSil(1L);
    }
}
