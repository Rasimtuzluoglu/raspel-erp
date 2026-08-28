package com.raspel.erp.controller.sistem;

import com.raspel.erp.controller.TestSecurityMocks;
import com.raspel.erp.entity.sistem.Belge;
import com.raspel.erp.repository.sistem.BelgeRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BelgeController.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@AutoConfigureMockMvc(addFilters = false)
@Import(TestSecurityMocks.class)
class BelgeControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private BelgeRepository belgeRepository;

    private Belge ornek() {
        return Belge.builder().id(1L).entityAdi("Fatura").entityId(5L)
                .dosyaAdi("fatura.pdf").url("/api/belgeler/indir/x.pdf")
                .sirketId(1L).olusturmaTarihi(LocalDateTime.now()).build();
    }

    @Test
    void shouldListKayitBelgeleri() throws Exception {
        when(belgeRepository.findByEntityAdiAndEntityIdAndSirketIdOrderByOlusturmaTarihiDesc("Fatura", 5L, 1L))
                .thenReturn(List.of(ornek()));
        mockMvc.perform(get("/api/belgeler/kayit/Fatura/5").requestAttr("sirketId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].dosyaAdi").value("fatura.pdf"));
    }

    @Test
    void shouldRejectEmptyUpload() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "empty.pdf", "application/pdf", new byte[0]);
        mockMvc.perform(multipart("/api/belgeler/yukle")
                        .file(file)
                        .param("entityAdi", "Fatura")
                        .param("entityId", "5")
                        .requestAttr("sirketId", 1L))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectInvalidType() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "virus.exe", "application/octet-stream", "data".getBytes());
        mockMvc.perform(multipart("/api/belgeler/yukle")
                        .file(file)
                        .param("entityAdi", "Fatura")
                        .param("entityId", "5")
                        .requestAttr("sirketId", 1L))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldDelete() throws Exception {
        when(belgeRepository.findById(1L)).thenReturn(Optional.of(ornek()));
        doNothing().when(belgeRepository).deleteById(1L);
        mockMvc.perform(delete("/api/belgeler/1").requestAttr("sirketId", 1L))
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldNotDeleteOtherCompanyDocument() throws Exception {
        when(belgeRepository.findById(1L)).thenReturn(Optional.of(ornek()));
        mockMvc.perform(delete("/api/belgeler/1").requestAttr("sirketId", 2L))
                .andExpect(status().isNotFound());
        verify(belgeRepository, never()).deleteById(anyLong());
    }

    @Test
    void shouldNotDownloadOtherCompanyDocument() throws Exception {
        when(belgeRepository.findByUrlEndingWith("x.pdf")).thenReturn(List.of(ornek()));
        mockMvc.perform(get("/api/belgeler/indir/x.pdf").requestAttr("sirketId", 2L))
                .andExpect(status().isNotFound());
    }
}
