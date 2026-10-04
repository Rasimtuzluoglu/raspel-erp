package com.raspel.erp.controller.sistem;

import com.raspel.erp.controller.TestSecurityMocks;
import com.raspel.erp.entity.sistem.Belge;
import com.raspel.erp.entity.ticaret.Fatura;
import com.raspel.erp.repository.sistem.BelgeRepository;
import com.raspel.erp.service.sistem.DosyaDepolamaService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import static org.mockito.ArgumentMatchers.anyString;
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
    @MockBean private DosyaDepolamaService dosyaDepolama;
    @MockBean private com.raspel.erp.repository.ticaret.FaturaRepository faturaRepository;
    @MockBean private com.raspel.erp.repository.ticaret.SiparisRepository siparisRepository;
    @MockBean private com.raspel.erp.repository.finans.CariHesapRepository cariHesapRepository;
    @MockBean private com.raspel.erp.repository.envanter.StokRepository stokRepository;
    @MockBean private com.raspel.erp.repository.ik.PersonelRepository personelRepository;

    private Belge ornek() {
        return Belge.builder().id(1L).entityAdi("Fatura").entityId(5L)
                .dosyaAdi("fatura.pdf").url("/api/belgeler/indir/x.pdf")
                .sirketId(1L).olusturmaTarihi(LocalDateTime.now()).build();
    }

    /** Gercek bir PDF imzasi; icerik imzasi dogrulamasi bunu bekliyor. */
    private static byte[] pdfBaytlari() {
        return new byte[]{'%', 'P', 'D', 'F', '-', '1', '.', '7', '\n', 1, 2, 3};
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
    void shouldListTumBelgeler() throws Exception {
        when(belgeRepository.findBySirketIdOrderByOlusturmaTarihiDesc(1L)).thenReturn(List.of(ornek()));
        mockMvc.perform(get("/api/belgeler").requestAttr("sirketId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].dosyaAdi").value("fatura.pdf"));
    }

    @Test
    void shouldListTumBelgelerSayfali() throws Exception {
        when(belgeRepository.findBySirketId(eq(1L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(ornek())));
        mockMvc.perform(get("/api/belgeler")
                        .param("page", "0").param("size", "20")
                        .requestAttr("sirketId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].dosyaAdi").value("fatura.pdf"));
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

    /**
     * C1: belge yüklenirken hedef kaydın o şirkete ait olduğu doğrulanır.
     * Sorgu dönen kaydın şirketi başka bir şirketse istek 404 ile reddedilir
     * ve dosya hiçbir şekilde diske yazılmaz.
     */
    @Test
    void shouldRejectUploadWhenTargetRecordIsOtherCompany() throws Exception {
        when(faturaRepository.findById(5L))
                .thenReturn(Optional.of(Fatura.builder().id(5L).sirketId(9L).build()));
        MockMultipartFile file = new MockMultipartFile("file", "f.pdf", "application/pdf", pdfBaytlari());

        mockMvc.perform(multipart("/api/belgeler/yukle")
                        .file(file)
                        .param("entityAdi", "Fatura")
                        .param("entityId", "5")
                        .requestAttr("sirketId", 1L))
                .andExpect(status().isNotFound());
        verify(dosyaDepolama, never()).kaydet(anyString(), any(MultipartFile.class));
    }

    @Test
    void shouldRejectUploadWhenTargetRecordMissing() throws Exception {
        when(cariHesapRepository.findById(77L)).thenReturn(Optional.empty());
        MockMultipartFile file = new MockMultipartFile("file", "f.pdf", "application/pdf", pdfBaytlari());

        mockMvc.perform(multipart("/api/belgeler/yukle")
                        .file(file)
                        .param("entityAdi", "CariHesap")
                        .param("entityId", "77")
                        .requestAttr("sirketId", 1L))
                .andExpect(status().isNotFound());
        verify(dosyaDepolama, never()).kaydet(anyString(), any(MultipartFile.class));
    }

    @Test
    void shouldRejectUploadForEntityOutsideWhitelist() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "f.pdf", "application/pdf", pdfBaytlari());

        mockMvc.perform(multipart("/api/belgeler/yukle")
                        .file(file)
                        .param("entityAdi", "Kullanici")
                        .param("entityId", "1")
                        .requestAttr("sirketId", 1L))
                .andExpect(status().isBadRequest());
        verify(dosyaDepolama, never()).kaydet(anyString(), any(MultipartFile.class));
    }

    @Test
    void shouldAcceptUploadWhenTargetRecordBelongsToCompany() throws Exception {
        when(faturaRepository.findById(5L))
                .thenReturn(Optional.of(Fatura.builder().id(5L).sirketId(1L).build()));
        when(dosyaDepolama.kaydet(anyString(), any(MultipartFile.class))).thenReturn("yeni.pdf");
        when(belgeRepository.save(any(Belge.class))).thenAnswer(i -> i.getArgument(0));
        MockMultipartFile file = new MockMultipartFile("file", "f.pdf", "application/pdf", pdfBaytlari());

        mockMvc.perform(multipart("/api/belgeler/yukle")
                        .file(file)
                        .param("entityAdi", "Fatura")
                        .param("entityId", "5")
                        .requestAttr("sirketId", 1L))
                .andExpect(status().isOk());
        verify(dosyaDepolama).kaydet(anyString(), any(MultipartFile.class));
    }

    /**
     * C7: indirme yanıtı nosniff taşımalı ve depolama katmanının ürettiği
     * tahmin edilemeyen adı kullanmalı; istemcinin gönderdiği orijinal dosya
     * adı (Content-Disposition'a sızabilir) yanıtta yer almamalı.
     */
    @Test
    void downloadShouldSetNoSniffAndStoredName() throws Exception {
        when(belgeRepository.findByUrlEndingWith("x.pdf")).thenReturn(List.of(ornek()));
        when(dosyaDepolama.getir(anyString(), anyString()))
                .thenReturn(new DosyaDepolamaService.DepolananDosya("data".getBytes(), "application/pdf"));

        mockMvc.perform(get("/api/belgeler/indir/x.pdf").requestAttr("sirketId", 1L))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("Content-Disposition", "inline; filename=\"x.pdf\""));
    }
}
