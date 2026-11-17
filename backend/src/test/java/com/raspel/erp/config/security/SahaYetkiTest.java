package com.raspel.erp.config.security;

import com.raspel.erp.dto.sistem.SahaOzetDTO;
import com.raspel.erp.service.sistem.SahaOzetService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Saha özet ucu rol erişimi: saha personeli (USER/SAHA) ve yönetici erişir,
 * satış rolü erişemez.
 */
@SpringBootTest(properties = {
        "management.health.redis.enabled=false",
        "management.health.rabbit.enabled=false",
        "management.health.db.enabled=false"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SahaYetkiTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SahaOzetService sahaOzetService;

    @Test
    void user_ve_saha_erisir() throws Exception {
        given(sahaOzetService.ozet(any(), any())).willReturn(SahaOzetDTO.builder().build());

        mockMvc.perform(get("/api/saha/ozet").with(user("u").roles("USER")))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/saha/ozet").with(user("s").roles("SAHA")))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/saha/ozet").with(user("a").roles("ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    void satis_rolu_403() throws Exception {
        mockMvc.perform(get("/api/saha/ozet").with(user("x").roles("SATIS")))
                .andExpect(status().isForbidden());
    }
}
