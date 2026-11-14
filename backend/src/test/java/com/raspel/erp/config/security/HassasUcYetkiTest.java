package com.raspel.erp.config.security;

import com.raspel.erp.dto.sube.DepoDTO;
import com.raspel.erp.service.ik.BordroHesaplamaService;
import com.raspel.erp.service.ik.MaasBordroService;
import com.raspel.erp.service.sistem.AuditLogService;
import com.raspel.erp.service.sube.DepoExportService;
import com.raspel.erp.service.sube.DepoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Hassas uçların ROL düzeyi erişim kuralları.
 *
 * <p>Güvenlik incelemesinde bulunan açıkların regresyon testleri:
 * <ul>
 *   <li>Bordro (maaş) uçları USER'a kapalı olmalı (tüm çalışan ücretleri).</li>
 *   <li>Denetim kayıtları yalnız ADMIN'e açık olmalı.</li>
 *   <li>Depo oluşturma/güncelleme yalnız ADMIN'de olmalı (silme zaten öyleydi).</li>
 * </ul>
 */
@SpringBootTest(properties = {
        "management.health.redis.enabled=false",
        "management.health.rabbit.enabled=false",
        "management.health.db.enabled=false"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class HassasUcYetkiTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MaasBordroService maasBordroService;
    @MockBean
    private BordroHesaplamaService bordroHesaplamaService;
    @MockBean
    private AuditLogService auditLogService;
    @MockBean
    private DepoService depoService;
    @MockBean
    private DepoExportService depoExportService;

    private static final String DEPO_GOVDE = "{\"ad\":\"Test Depo\",\"subeId\":1}";

    @Test
    void bordro_listesi_userA_Kapali() throws Exception {
        mockMvc.perform(get("/api/maas-bordro").with(user("u").roles("USER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void bordro_olusturma_userA_Kapali() throws Exception {
        mockMvc.perform(post("/api/maas-bordro")
                        .with(user("u").roles("USER"))
                        .requestAttr("sirketId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"personelId\":1,\"yil\":2026,\"ay\":1,\"brutMaas\":10000}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void bordro_guncelleme_userA_Kapali() throws Exception {
        mockMvc.perform(put("/api/maas-bordro/5")
                        .with(user("u").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"personelId\":1,\"yil\":2026,\"ay\":1,\"brutMaas\":10000}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void bordro_muhasebeYeAcik() throws Exception {
        mockMvc.perform(get("/api/maas-bordro").with(user("m").roles("MUHASEBE")))
                .andExpect(result -> {
                    org.junit.jupiter.api.Assertions.assertNotEquals(
                            403, result.getResponse().getStatus());
                });
    }

    @Test
    void auditLog_userA_Kapali() throws Exception {
        mockMvc.perform(get("/api/audit-log").with(user("u").roles("USER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void auditLog_adminAcik() throws Exception {
        mockMvc.perform(get("/api/audit-log").with(user("a").roles("ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    void depoOlusturma_userA_Kapali() throws Exception {
        mockMvc.perform(post("/api/depolar")
                        .with(user("u").roles("USER"))
                        .requestAttr("sirketId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(DEPO_GOVDE))
                .andExpect(status().isForbidden());
    }

    @Test
    void depoGuncelleme_userA_Kapali() throws Exception {
        mockMvc.perform(put("/api/depolar/3")
                        .with(user("u").roles("USER"))
                        .requestAttr("sirketId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(DEPO_GOVDE))
                .andExpect(status().isForbidden());
    }

    @Test
    void depoOlusturma_adminAcik() throws Exception {
        given(depoService.olustur(any(DepoDTO.class)))
                .willReturn(DepoDTO.builder().id(9L).ad("Test Depo").subeId(1L).build());
        mockMvc.perform(post("/api/depolar")
                        .with(user("a").roles("ADMIN"))
                        .requestAttr("sirketId", 1L)
                        .requestAttr("kullaniciId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(DEPO_GOVDE))
                .andExpect(status().isCreated());
    }
}
