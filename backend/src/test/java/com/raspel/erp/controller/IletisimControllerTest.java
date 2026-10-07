package com.raspel.erp.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.raspel.erp.config.TenantChecker;
import com.raspel.erp.entity.finans.CariHesap;
import com.raspel.erp.repository.finans.CariHesapRepository;
import com.raspel.erp.service.sistem.EmailPolitikaService;
import com.raspel.erp.service.sistem.EmailService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.raspel.erp.controller.sistem.IletisimController;

@WebMvcTest(IletisimController.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@AutoConfigureMockMvc(addFilters = false)
@Import(TestSecurityMocks.class)
class IletisimControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private CariHesapRepository cariHesapRepository;
    @MockBean private TenantChecker tenantChecker;
    @MockBean private EmailService emailService;
    @MockBean private EmailPolitikaService emailPolitikaService;

    private CariHesap cari(String email, String telefon) {
        return CariHesap.builder().id(1L).ad("ABC").sirketId(1L).email(email).telefon(telefon).build();
    }

    @Test
    void mailGonder_basarili() throws Exception {
        when(cariHesapRepository.findById(1L)).thenReturn(Optional.of(cari("a@b.com", "05551112233")));
        when(emailService.htmlGonder(eq("a@b.com"), anyString(), anyString())).thenReturn(true);

        mockMvc.perform(post("/api/iletisim/mail")
                        .requestAttr("sirketId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "cariHesapId", 1, "konu", "Merhaba", "mesaj", "Deneme"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.durum").value("GONDERILDI"));

        verify(emailPolitikaService).aliciDogrula("a@b.com", 1L);
    }

    @Test
    void mailGonder_emailYoksa400() throws Exception {
        when(cariHesapRepository.findById(1L)).thenReturn(Optional.of(cari(null, "05551112233")));

        mockMvc.perform(post("/api/iletisim/mail")
                        .requestAttr("sirketId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "cariHesapId", 1, "mesaj", "Deneme"))))
                .andExpect(status().isBadRequest());

        verify(emailService, never()).htmlGonder(any(), any(), any());
    }

    @Test
    void mailGonder_mesajBosIse400() throws Exception {
        mockMvc.perform(post("/api/iletisim/mail")
                        .requestAttr("sirketId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "cariHesapId", 1, "mesaj", "  "))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void whatsapp_linkUretir() throws Exception {
        when(cariHesapRepository.findById(1L)).thenReturn(Optional.of(cari("a@b.com", "0555 111 22 33")));

        mockMvc.perform(get("/api/iletisim/whatsapp/1").requestAttr("sirketId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.link").value("https://wa.me/905551112233"));
    }

    @Test
    void whatsapp_telefonYoksa400() throws Exception {
        when(cariHesapRepository.findById(1L)).thenReturn(Optional.of(cari("a@b.com", null)));

        mockMvc.perform(get("/api/iletisim/whatsapp/1").requestAttr("sirketId", 1L))
                .andExpect(status().isBadRequest());
    }
}
