package com.raspel.erp.controller.ticaret;

import com.raspel.erp.controller.TestSecurityMocks;
import com.raspel.erp.dto.ticaret.TekrarlayanFaturaDTO;
import com.raspel.erp.service.ticaret.TekrarlayanFaturaService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TekrarlayanFaturaController.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@AutoConfigureMockMvc(addFilters = false)
@Import(TestSecurityMocks.class)
class TekrarlayanFaturaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TekrarlayanFaturaService tekrarlayanFaturaService;

    @Test
    void listele_donar() throws Exception {
        when(tekrarlayanFaturaService.listele(any())).thenReturn(List.of(
                TekrarlayanFaturaDTO.builder().id(1L).tur("SATIS").aktif(true).build()));

        mockMvc.perform(get("/api/tekrarlayan-faturalar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tur").value("SATIS"));
    }

    @Test
    void getir_donar() throws Exception {
        when(tekrarlayanFaturaService.getir(1L)).thenReturn(TekrarlayanFaturaDTO.builder().id(1L).build());

        mockMvc.perform(get("/api/tekrarlayan-faturalar/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void sil_siler() throws Exception {
        mockMvc.perform(delete("/api/tekrarlayan-faturalar/1"))
                .andExpect(status().isNoContent());
        verify(tekrarlayanFaturaService).sil(1L);
    }

    @Test
    void uret_faturaUretir() throws Exception {
        mockMvc.perform(post("/api/tekrarlayan-faturalar/1/uret"))
                .andExpect(status().isOk());
        verify(tekrarlayanFaturaService).faturaUret(1L);
    }
}
