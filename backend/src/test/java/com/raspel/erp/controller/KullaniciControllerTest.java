package com.raspel.erp.controller;

import com.raspel.erp.dto.KullaniciDTO;
import com.raspel.erp.dto.LoginRequest;
import com.raspel.erp.dto.LoginResponse;
import com.raspel.erp.service.KullaniciService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.test.annotation.DirtiesContext;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(KullaniciController.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@AutoConfigureMockMvc(addFilters = false)
@Import(TestSecurityMocks.class)
class KullaniciControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    
    @MockBean
    private KullaniciService kullaniciService;

    @Test
    void shouldGetAll() throws Exception {
        var list = List.of(KullaniciDTO.builder().id(1L).username("admin").displayName("Admin").build());
        when(kullaniciService.tumunuGetir()).thenReturn(list);

        mockMvc.perform(get("/api/kullanicilar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].username").value("admin"));
    }

    @Test
    void shouldGetById() throws Exception {
        var dto = KullaniciDTO.builder().id(1L).username("admin").displayName("Admin").build();
        when(kullaniciService.getir(1L)).thenReturn(dto);

        mockMvc.perform(get("/api/kullanicilar/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.username").value("admin"));
    }

    @Test
    void shouldReturnNotFoundWhenGetById() throws Exception {
        when(kullaniciService.getir(anyLong())).thenThrow(new RuntimeException("Kullanıcı bulunamadı: 999"));

        mockMvc.perform(get("/api/kullanicilar/999"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldCreate() throws Exception {
        var dto = KullaniciDTO.builder().id(1L).username("newuser").displayName("New User").password("pass").build();
        when(kullaniciService.olustur(any(KullaniciDTO.class))).thenReturn(dto);

        mockMvc.perform(post("/api/kullanicilar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L));
    }

    @Test
    void shouldUpdate() throws Exception {
        var dto = KullaniciDTO.builder().id(1L).username("admin").displayName("Updated").build();
        when(kullaniciService.guncelle(eq(1L), any(KullaniciDTO.class))).thenReturn(dto);

        mockMvc.perform(put("/api/kullanicilar/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Updated"));
    }

    @Test
    void shouldDelete() throws Exception {
        doNothing().when(kullaniciService).sil(1L);

        mockMvc.perform(delete("/api/kullanicilar/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldLogin() throws Exception {
        var request = new LoginRequest("admin", "pass", "Test Company");
        var response = LoginResponse.builder().id(1L).username("admin").token("jwt-token").build();
        when(kullaniciService.giris(any(LoginRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/kullanicilar/giris")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"));
    }

    @Test
    void shouldLoginWithInvalidCredentials() throws Exception {
        var request = new LoginRequest("admin", "wrong", null);
        when(kullaniciService.giris(any(LoginRequest.class)))
                .thenThrow(new RuntimeException("Kullanıcı adı veya şifre hatalı"));

        mockMvc.perform(post("/api/kullanicilar/giris")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}








