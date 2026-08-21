package com.raspel.erp.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.raspel.erp.controller.ik.PersonelMasrafTalepController;
import com.raspel.erp.dto.ik.PersonelMasrafTalepDTO;
import com.raspel.erp.service.ik.PersonelMasrafTalepService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PersonelMasrafTalepController.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@AutoConfigureMockMvc(addFilters = false)
@Import(TestSecurityMocks.class)
class PersonelMasrafTalepControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PersonelMasrafTalepService talepService;

    @Test
    void shouldGetBekleyenler() throws Exception {
        var list = List.of(PersonelMasrafTalepDTO.builder().id(1L).tutar(BigDecimal.valueOf(150)).durum("BEKLEMEDE").build());
        when(talepService.bekleyenleriGetir(1L)).thenReturn(list);

        mockMvc.perform(get("/api/personel-masraf-talepler/bekleyenler").requestAttr("sirketId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tutar").value(150));
    }

    @Test
    void shouldCreateTalep() throws Exception {
        var dto = PersonelMasrafTalepDTO.builder()
                .tutar(BigDecimal.valueOf(250))
                .kategori("YAKIT")
                .aciklama("Benzin")
                .build();

        when(talepService.talepOlustur(any(PersonelMasrafTalepDTO.class), eq(1L), eq(2L))).thenReturn(dto);

        mockMvc.perform(post("/api/personel-masraf-talepler")
                        .requestAttr("sirketId", 1L)
                        .requestAttr("kullaniciId", 2L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tutar").value(250));
    }

    @Test
    void shouldApproveTalep() throws Exception {
        var dto = PersonelMasrafTalepDTO.builder().id(1L).durum("ONAYLANDI").build();
        when(talepService.onayla(eq(1L), any(), any())).thenReturn(dto);

        mockMvc.perform(patch("/api/personel-masraf-talepler/1/onayla")
                        .requestAttr("sirketId", 1L)
                        .requestAttr("username", "admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.durum").value("ONAYLANDI"));
    }
}
