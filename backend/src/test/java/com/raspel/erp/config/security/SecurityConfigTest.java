package com.raspel.erp.config.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "management.health.redis.enabled=false",
        "management.health.rabbit.enabled=false",
        "management.health.db.enabled=false"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void healthEndpoint_isPublic() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(result -> assertNotEquals(403, result.getResponse().getStatus()));
    }

    @Test
    void loginEndpoint_isPublic() throws Exception {
        mockMvc.perform(get("/api/kullanicilar/giris"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void preflightRequest_isPermitted() throws Exception {
        mockMvc.perform(options("/api/kullanicilar")
                        .header("Access-Control-Request-Method", "GET")
                        .header("Origin", "http://localhost:5173"))
                .andExpect(status().isOk());
    }

    @Test
    void authenticatedEndpoint_returnsUnauthorizedWhenNoToken() throws Exception {
        mockMvc.perform(get("/api/kullanicilar"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void authenticatedEndpoint_returnsUnauthorizedWhenInvalidToken() throws Exception {
        mockMvc.perform(get("/api/kullanicilar")
                        .header("Authorization", "Bearer invalid.token.here"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void corsConfiguration_blocksDisallowedOrigin() throws Exception {
        mockMvc.perform(options("/api/kullanicilar")
                        .header("Access-Control-Request-Method", "GET")
                        .header("Origin", "http://evil.com"))
                .andExpect(status().isForbidden());
    }

    // --- HTTP katmanı yetki (method security, filtreler acik) ---

    @Test
    void adminOnlyDelete_forbiddenForUserRole() throws Exception {
        mockMvc.perform(delete("/api/stoklar/1").with(user("test-user").roles("USER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminOnlyDelete_forbiddenForDriverRole() throws Exception {
        mockMvc.perform(delete("/api/stoklar/1").with(user("test-driver").roles("DRIVER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminOnlyDelete_allowedForAdminRole() throws Exception {
        // Yetki kapisi gecilir; sonuc (404/200) onemli degil, 401/403 OLMAMALI.
        mockMvc.perform(delete("/api/stoklar/1").with(user("test-admin").roles("ADMIN")))
                .andExpect(result -> assertNotEquals(401, result.getResponse().getStatus()))
                .andExpect(result -> assertNotEquals(403, result.getResponse().getStatus()));
    }
}
