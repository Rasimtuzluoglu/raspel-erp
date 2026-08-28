package com.raspel.erp.controller;

import com.raspel.erp.dto.sistem.KullaniciDTO;
import com.raspel.erp.dto.sistem.LoginRequest;
import com.raspel.erp.dto.sistem.LoginResponse;
import com.raspel.erp.exception.BusinessException;
import com.raspel.erp.exception.ResourceNotFoundException;
import com.raspel.erp.service.sistem.KullaniciService;
import com.raspel.erp.service.sistem.AktifOturumService;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.annotation.DirtiesContext;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.raspel.erp.controller.sistem.KullaniciController;

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

    @MockBean
    private AktifOturumService aktifOturumService;

    @Test
    void shouldGetAll() throws Exception {
        var list = List.of(KullaniciDTO.builder().id(1L).username("admin").displayName("Admin").build());
        when(kullaniciService.tumunuGetir(any(Pageable.class))).thenReturn(new PageImpl<>(list));

        mockMvc.perform(get("/api/kullanicilar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1L))
                .andExpect(jsonPath("$.content[0].username").value("admin"));
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
        when(kullaniciService.getir(anyLong())).thenThrow(new ResourceNotFoundException("Kullanıcı", 999L));

        mockMvc.perform(get("/api/kullanicilar/999"))
                .andExpect(status().isNotFound());
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
        var request = new LoginRequest("admin", "pass", "Test Company", null);
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
        var request = new LoginRequest("admin", "wrong", null, null);
        when(kullaniciService.giris(any(LoginRequest.class)))
                .thenThrow(new BusinessException("Kullanıcı adı veya şifre hatalı"));

        mockMvc.perform(post("/api/kullanicilar/giris")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}





