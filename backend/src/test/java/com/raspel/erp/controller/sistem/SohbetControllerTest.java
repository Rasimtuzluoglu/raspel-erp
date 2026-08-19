package com.raspel.erp.controller.sistem;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.raspel.erp.controller.TestSecurityMocks;
import com.raspel.erp.dto.sistem.AISorguSonucDTO;
import com.raspel.erp.dto.sistem.SohbetMesajDTO;
import com.raspel.erp.service.sistem.SohbetService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SohbetController.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@AutoConfigureMockMvc(addFilters = false)
@Import(TestSecurityMocks.class)
class SohbetControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private SohbetService sohbetService;

    @Test
    void shouldGetSonMesajlar() throws Exception {
        var list = List.of(SohbetMesajDTO.builder().id(1L).mesaj("Test mesaj").build());
        when(sohbetService.sonMesajlar(1L)).thenReturn(list);

        mockMvc.perform(get("/api/sohbet").requestAttr("sirketId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].mesaj").value("Test mesaj"));
    }

    @Test
    void shouldMesajGonder() throws Exception {
        SohbetMesajDTO dto = SohbetMesajDTO.builder().mesaj("Merhaba").build();
        when(sohbetService.mesajGonder(any(), eq(1L), eq(2L), eq("Admin"))).thenReturn(dto);

        mockMvc.perform(post("/api/sohbet")
                        .requestAttr("sirketId", 1L)
                        .requestAttr("kullaniciId", 2L)
                        .requestAttr("displayName", "Admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mesaj").value("Merhaba"));
    }

    @Test
    void shouldAISorgu() throws Exception {
        AISorguSonucDTO res = AISorguSonucDTO.builder()
                .soru("En çok ciro yapan müşteri")
                .cevapMetni("Müşteri listelendi")
                .intent("CIRO_MUSTERI")
                .build();
        when(sohbetService.aiSorgula(eq("En çok ciro yapan müşteri"), eq(1L))).thenReturn(res);

        mockMvc.perform(post("/api/sohbet/ai-sorgu")
                        .requestAttr("sirketId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("soru", "En çok ciro yapan müşteri"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.intent").value("CIRO_MUSTERI"));
    }
}
