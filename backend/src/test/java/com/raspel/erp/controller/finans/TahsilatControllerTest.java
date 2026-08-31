package com.raspel.erp.controller.finans;

import com.raspel.erp.controller.TestSecurityMocks;
import com.raspel.erp.dto.finans.TahsilatDTO;
import com.raspel.erp.service.finans.TahsilatService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TahsilatController.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@AutoConfigureMockMvc(addFilters = false)
@Import(TestSecurityMocks.class)
class TahsilatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TahsilatService tahsilatService;

    @Test
    void ozet_donar() throws Exception {
        TahsilatDTO dto = TahsilatDTO.builder()
                .toplamAlacak(new BigDecimal("1000.00"))
                .acikFaturaSayisi(3)
                .build();
        when(tahsilatService.ozetGetir(null)).thenReturn(dto);

        mockMvc.perform(get("/api/tahsilat"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.toplamAlacak").value(1000.00))
                .andExpect(jsonPath("$.acikFaturaSayisi").value(3));
    }

    @Test
    void hatirlat_gonderilenSayiDonar() throws Exception {
        when(tahsilatService.hatirlat(eq(5L), eq(null))).thenReturn(2);

        mockMvc.perform(post("/api/tahsilat/5/hatirlat"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gonderilen").value(2));
    }
}
