package com.raspel.erp.config.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void healthEndpoint_isPublic() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
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
                .andExpect(status().isForbidden());
    }

    @Test
    void authenticatedEndpoint_returnsForbiddenWhenInvalidToken() throws Exception {
        mockMvc.perform(get("/api/kullanicilar")
                        .header("Authorization", "Bearer invalid.token.here"))
                .andExpect(status().isForbidden());
    }

    @Test
    void corsConfiguration_blocksDisallowedOrigin() throws Exception {
        mockMvc.perform(options("/api/kullanicilar")
                        .header("Access-Control-Request-Method", "GET")
                        .header("Origin", "http://evil.com"))
                .andExpect(status().isForbidden());
    }
}
