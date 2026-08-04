package com.raspel.erp.controller;

import com.raspel.erp.dto.sistem.NotDTO;
import com.raspel.erp.service.sistem.NotService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.raspel.erp.controller.sistem.NotController;

@WebMvcTest(NotController.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@AutoConfigureMockMvc(addFilters = false)
@Import(TestSecurityMocks.class)
class NotControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private NotService notService;

    private NotDTO ornekNot() {
        return NotDTO.builder()
                .id(1L)
                .baslik("Toplantı Notu")
                .icerik("Yarın 10:00")
                .onemDerecesi("YUKSEK")
                .renk("MAVI")
                .kullaniciId(1L)
                .olusturmaTarihi(LocalDateTime.now())
                .build();
    }

    @Test
    void shouldGetAll() throws Exception {
        when(notService.tumunuGetir(anyLong(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(ornekNot())));

        mockMvc.perform(get("/api/notlar").requestAttr("sirketId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].baslik").value("Toplantı Notu"))
                .andExpect(jsonPath("$.content[0].renk").value("MAVI"));
    }

    @Test
    void shouldGetById() throws Exception {
        when(notService.idyeGoreGetir(1L)).thenReturn(ornekNot());

        mockMvc.perform(get("/api/notlar/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.baslik").value("Toplantı Notu"));
    }

    @Test
    void shouldCreate() throws Exception {
        NotDTO dto = ornekNot();
        when(notService.olustur(any(NotDTO.class), anyLong(), anyLong())).thenReturn(dto);

        mockMvc.perform(post("/api/notlar")
                        .requestAttr("sirketId", 1L)
                        .requestAttr("kullaniciId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.baslik").value("Toplantı Notu"));
    }

    @Test
    void shouldUpdate() throws Exception {
        NotDTO dto = ornekNot();
        when(notService.guncelle(eq(1L), any(NotDTO.class))).thenReturn(dto);

        mockMvc.perform(put("/api/notlar/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.onemDerecesi").value("YUKSEK"));
    }

    @Test
    void shouldDelete() throws Exception {
        doNothing().when(notService).sil(1L);

        mockMvc.perform(delete("/api/notlar/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldReturn400OnEmptyBaslik() throws Exception {
        NotDTO gecersiz = NotDTO.builder().baslik("").build();

        mockMvc.perform(post("/api/notlar")
                        .requestAttr("sirketId", 1L)
                        .requestAttr("kullaniciId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(gecersiz)))
                .andExpect(status().isBadRequest());
    }
}