package com.raspel.erp.config;

import com.raspel.erp.config.security.JwtUtil;
import com.raspel.erp.entity.sistem.Kullanici;
import com.raspel.erp.entity.ticaret.Fatura;
import com.raspel.erp.repository.sistem.KullaniciRepository;
import com.raspel.erp.repository.ticaret.FaturaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TenantIsolationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private KullaniciRepository kullaniciRepository;
    @Autowired private FaturaRepository faturaRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtUtil jwtUtil;

    private String sirketBToken;
    private Long sirketAFaturaId;

    @BeforeEach
    void setUp() {
        Kullanici userA = kullaniciRepository.findByUsername("tenantA").orElseGet(() -> {
            Kullanici u = new Kullanici();
            u.setUsername("tenantA");
            u.setPassword(passwordEncoder.encode("test"));
            u.setDisplayName("Tenant A");
            u.setRole("USER"); u.setActive(true); u.setSirketId(1L);
            return kullaniciRepository.save(u);
        });

        Kullanici userB = kullaniciRepository.findByUsername("tenantB").orElseGet(() -> {
            Kullanici u = new Kullanici();
            u.setUsername("tenantB");
            u.setPassword(passwordEncoder.encode("test"));
            u.setDisplayName("Tenant B");
            u.setRole("USER"); u.setActive(true); u.setSirketId(2L);
            return kullaniciRepository.save(u);
        });

        sirketBToken = jwtUtil.generateToken(userB, userB.getSirketId(), "Sirket B");

        Fatura fatura = faturaRepository.findAll().stream()
                .filter(f -> f.getSirketId() != null && f.getSirketId().equals(1L))
                .findFirst().orElseGet(() -> {
                    Fatura f = new Fatura();
                    f.setTarih(LocalDate.now()); f.setTur("SATIS"); f.setDurum("TASLAK");
                    f.setAraToplam(100.0); f.setKdv(20.0); f.setGenelToplam(120.0);
                    f.setSirketId(1L);
                    return faturaRepository.save(f);
                });
        sirketAFaturaId = fatura.getId();
    }

    @Test
    void tenantIsolation_shouldReturn404_whenAccessingOtherCompanyRecord() throws Exception {
        mockMvc.perform(get("/api/faturalar/" + sirketAFaturaId)
                        .header("Authorization", "Bearer " + sirketBToken)
                        .header("X-Sirket-Id", "2")
                        .accept(org.springframework.http.MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }
}

