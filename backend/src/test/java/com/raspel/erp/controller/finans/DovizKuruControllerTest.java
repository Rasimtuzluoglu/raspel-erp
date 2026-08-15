package com.raspel.erp.controller.finans;

import com.raspel.erp.controller.TestSecurityMocks;
import com.raspel.erp.dto.finans.DovizKuruDTO;
import com.raspel.erp.entity.finans.DovizKuru;
import com.raspel.erp.service.finans.DovizKuruService;
import com.raspel.erp.service.sistem.TcmbKurService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DovizKuruController.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@AutoConfigureMockMvc(addFilters = false)
@Import(TestSecurityMocks.class)
class DovizKuruControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private DovizKuruService dovizKuruService;
    @MockBean private TcmbKurService tcmbKurService;

    @Test
    void shouldGetKurlar() throws Exception {
        when(tcmbKurService.tumKurlariGetir()).thenReturn(List.of(DovizKuru.builder()
                .dovizKodu("USD").dovizAdi("ABD Doları").alisKuru(new BigDecimal("34.50")).satisKuru(new BigDecimal("34.60")).build()));
        mockMvc.perform(get("/api/doviz/kurlar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].dovizKodu").value("USD"));
    }

    @Test
    void shouldGuncelle() throws Exception {
        doNothing().when(tcmbKurService).tcmbKurlariniGuncelle();
        when(tcmbKurService.tumKurlariGetir()).thenReturn(List.of());
        mockMvc.perform(post("/api/doviz/guncelle"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldCevir() throws Exception {
        when(tcmbKurService.cevir(any(BigDecimal.class), eq("USD"), eq("TRY")))
                .thenReturn(new BigDecimal("345.00"));
        mockMvc.perform(get("/api/doviz/cevir")
                        .param("tutar", "10")
                        .param("kaynak", "USD")
                        .param("hedef", "TRY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sonuc").value(345.00));
    }

    @Test
    void shouldKurKaydet() throws Exception {
        when(dovizKuruService.kurEkleVeyaGuncelle(any(DovizKuruDTO.class)))
                .thenReturn(DovizKuruDTO.builder().id(1L).dovizKodu("USD").build());
        mockMvc.perform(post("/api/doviz")
                        .contentType("application/json")
                        .content("{\"dovizKodu\":\"USD\",\"alisKuru\":34.50,\"satisKuru\":34.60}"))
                .andExpect(status().isCreated());
    }
}
