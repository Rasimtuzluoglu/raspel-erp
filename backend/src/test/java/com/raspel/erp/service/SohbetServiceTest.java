package com.raspel.erp.service;

import com.raspel.erp.dto.sistem.AISorguSonucDTO;
import com.raspel.erp.dto.sistem.SohbetMesajDTO;
import com.raspel.erp.entity.finans.Banka;
import com.raspel.erp.entity.finans.CariHesap;
import com.raspel.erp.entity.finans.Kasa;
import com.raspel.erp.entity.sistem.SohbetMesaj;
import com.raspel.erp.entity.ticaret.Fatura;
import com.raspel.erp.repository.envanter.StokRepository;
import com.raspel.erp.repository.finans.BankaRepository;
import com.raspel.erp.repository.finans.CariHesapRepository;
import com.raspel.erp.repository.finans.KasaRepository;
import com.raspel.erp.repository.sistem.SohbetMesajRepository;
import com.raspel.erp.repository.ticaret.FaturaRepository;
import com.raspel.erp.service.sistem.SohbetService;
import com.raspel.erp.service.sistem.SohbetHizSiniri;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SohbetServiceTest {

    @Mock private SohbetMesajRepository sohbetMesajRepository;
    @Mock private com.raspel.erp.repository.sistem.SohbetGenelOkumaRepository genelOkumaRepository;
    @Mock private SimpMessagingTemplate messagingTemplate;
    @Mock private FaturaRepository faturaRepository;
    @Mock private CariHesapRepository cariHesapRepository;
    @Mock private StokRepository stokRepository;
    @Mock private KasaRepository kasaRepository;
    @Mock private BankaRepository bankaRepository;
    @Mock private com.raspel.erp.service.sistem.AiConfigService aiConfigService;
    @Mock private com.raspel.erp.service.sistem.LlmClientService llmClientService;
    @Mock private com.raspel.erp.service.sistem.DosyaDepolamaService dosyaDepolama;
    @Mock private SohbetHizSiniri hizSiniri;
    @Mock private com.raspel.erp.config.TenantChecker tenantChecker;

    @InjectMocks private SohbetService sohbetService;

    @Test
    void sonMesajlar_returnsReversedList() {
        SohbetMesaj m1 = SohbetMesaj.builder().id(1L).mesaj("Mesaj 1").olusturmaTarihi(Instant.now()).build();
        SohbetMesaj m2 = SohbetMesaj.builder().id(2L).mesaj("Mesaj 2").olusturmaTarihi(Instant.now()).build();
        when(sohbetMesajRepository.findBySirketIdAndOdaIdIsNullOrderByIdDesc(eq(1L), any()))
                .thenReturn(new java.util.ArrayList<>(List.of(m2, m1)));

        List<SohbetMesajDTO> result = sohbetService.sonMesajlar(1L);
        assertEquals(2, result.size());
        assertEquals("Mesaj 1", result.get(0).getMesaj());
    }

    @Test
    void sonMesajlar_cursorVerilinceEskiSayfayiDoner() {
        when(sohbetMesajRepository.findBySirketIdAndOdaIdIsNullAndIdLessThanOrderByIdDesc(eq(1L), eq(5L), any()))
                .thenReturn(new java.util.ArrayList<>(List.of(
                        SohbetMesaj.builder().id(4L).mesaj("eski").build())));

        List<SohbetMesajDTO> result = sohbetService.sonMesajlar(1L, 5L, 50);
        assertEquals(1, result.size());
        assertEquals(4L, result.get(0).getId());
    }

    @Test
    void genelOkunmamisSayisi_sonOkunanIdIleSayar() {
        when(genelOkumaRepository.findBySirketIdAndKullaniciId(1L, 5L)).thenReturn(java.util.Optional.of(
                com.raspel.erp.entity.sistem.SohbetGenelOkuma.builder()
                        .sirketId(1L).kullaniciId(5L).sonOkunanId(7L).build()));
        when(sohbetMesajRepository.countBySirketIdAndOdaIdIsNullAndIdGreaterThan(1L, 7L)).thenReturn(3L);
        assertEquals(3L, sohbetService.genelOkunmamisSayisi(1L, 5L));
    }

    @Test
    void genelOkunmamisSayisi_okumaKaydiYoksaSifir() {
        when(genelOkumaRepository.findBySirketIdAndKullaniciId(1L, 5L)).thenReturn(java.util.Optional.empty());
        assertEquals(0L, sohbetService.genelOkunmamisSayisi(1L, 5L));
    }

    @Test
    void genelOkunduIsaretle_ileriYondeGunceller() {
        when(genelOkumaRepository.findBySirketIdAndKullaniciId(1L, 5L)).thenReturn(java.util.Optional.of(
                com.raspel.erp.entity.sistem.SohbetGenelOkuma.builder()
                        .sirketId(1L).kullaniciId(5L).sonOkunanId(7L).build()));

        sohbetService.genelOkunduIsaretle(1L, 5L, 9L);

        verify(genelOkumaRepository).save(argThat(o -> o.getSonOkunanId().equals(9L)));
    }

    @Test
    void genelOkunduIsaretle_geriyeYazmaz() {
        when(genelOkumaRepository.findBySirketIdAndKullaniciId(1L, 5L)).thenReturn(java.util.Optional.of(
                com.raspel.erp.entity.sistem.SohbetGenelOkuma.builder()
                        .sirketId(1L).kullaniciId(5L).sonOkunanId(9L).build()));

        sohbetService.genelOkunduIsaretle(1L, 5L, 4L);

        verify(genelOkumaRepository, never()).save(any());
    }

    @Test
    void mesajGonder_disSiteDosyaUrlReddedilir() {
        SohbetMesajDTO dto = SohbetMesajDTO.builder().mesaj("x")
                .dosyaUrl("https://evil.example/tracker.png").build();

        assertThrows(com.raspel.erp.exception.BusinessException.class,
                () -> sohbetService.mesajGonder(dto, 1L, 2L, "Ali"));
        verify(sohbetMesajRepository, never()).save(any(SohbetMesaj.class));
    }

    @Test
    void mesajGonder_kendiYuklemeUrlKabulEdilir() {
        SohbetMesajDTO dto = SohbetMesajDTO.builder().mesaj("fatura")
                .dosyaUrl("/api/uploads/sohbet/abc123.png").build();
        when(sohbetMesajRepository.save(any(SohbetMesaj.class))).thenAnswer(inv -> {
            SohbetMesaj m = inv.getArgument(0);
            m.setId(5L);
            return m;
        });

        SohbetMesajDTO sonuc = sohbetService.mesajGonder(dto, 1L, 2L, "Ali");
        assertEquals("/api/uploads/sohbet/abc123.png", sonuc.getDosyaUrl());
    }

    /**
     * Faz 8: AI konusma bellegi kullanici bazlidir. Bir kullanicinin sorusu
     * baska kullanicinin system prompt'una sizmamali.
     */
    @Test
    void aiSorgula_hafizaKullaniciBazli() {
        when(aiConfigService.getConfig(1L)).thenReturn(com.raspel.erp.dto.sistem.AiConfigDTO.builder()
                .provider("OPENAI").model("gpt-4o").aktif(true).durum("AKTIF").build());
        when(aiConfigService.getDecryptedKey(1L)).thenReturn("key");
        when(llmClientService.sendQuery(any(), any(), any(), any(), any()))
                .thenReturn("ilk cevap", "ikinci cevap");

        sohbetService.aiSorgula("gizli soru A", 1L, 10L);
        sohbetService.aiSorgula("soru B", 1L, 20L);

        ArgumentCaptor<String> prompt = ArgumentCaptor.forClass(String.class);
        verify(llmClientService, times(2)).sendQuery(any(), any(), any(), prompt.capture(), any());
        // Ikinci kullanicinin prompt'unda birinci kullanicinin sorusu/cevabi yok.
        assertFalse(prompt.getAllValues().get(1).contains("gizli soru A"));
        assertFalse(prompt.getAllValues().get(1).contains("ilk cevap"));
        // Ayni kullanici tekrar sorunca kendi gecmisi gelir.
        sohbetService.aiSorgula("soru C", 1L, 10L);
        ArgumentCaptor<String> prompt2 = ArgumentCaptor.forClass(String.class);
        verify(llmClientService, times(3)).sendQuery(any(), any(), any(), prompt2.capture(), any());
        assertTrue(prompt2.getAllValues().get(2).contains("gizli soru A"));
    }

    @Test
    void mesajGonder_savesAndPublishes() {
        SohbetMesajDTO dto = SohbetMesajDTO.builder().mesaj("Merhaba").build();
        SohbetMesaj saved = SohbetMesaj.builder().id(1L).sirketId(1L).kullaniciId(2L).kullaniciAd("Admin").mesaj("Merhaba").build();
        when(sohbetMesajRepository.save(any(SohbetMesaj.class))).thenReturn(saved);

        SohbetMesajDTO result = sohbetService.mesajGonder(dto, 1L, 2L, "Admin");
        assertEquals("Merhaba", result.getMesaj());
        verify(sohbetMesajRepository, times(1)).save(any(SohbetMesaj.class));
    }

    @Test
    void mesajSil_deletesWhenSirketMatches() {
        SohbetMesaj m = SohbetMesaj.builder().id(7L).sirketId(1L).mesaj("Silinecek").build();
        when(sohbetMesajRepository.findById(7L)).thenReturn(java.util.Optional.of(m));

        sohbetService.mesajSil(7L, 1L);

        verify(sohbetMesajRepository).delete(m);
    }

    @Test
    void mesajSil_throwsWhenBaskaSirket() {
        SohbetMesaj m = SohbetMesaj.builder().id(7L).sirketId(2L).mesaj("Gizli").build();
        when(sohbetMesajRepository.findById(7L)).thenReturn(java.util.Optional.of(m));

        assertThrows(com.raspel.erp.exception.BusinessException.class, () -> sohbetService.mesajSil(7L, 1L));
        verify(sohbetMesajRepository, never()).delete(any(SohbetMesaj.class));
    }

    @Test
    void aiSorgula_ciroIntent() {
        when(faturaRepository.cariBazindaCiroTop(eq(1L), any(), any(), any()))
                .thenReturn(List.of(Map.of("cariAd", "ABC Ltd.", "ciro", BigDecimal.valueOf(150000))));

        AISorguSonucDTO res = sohbetService.aiSorgula("Bu ay en çok ciro yapan müşterilerimiz kimler?", 1L);
        assertEquals("CIRO_MUSTERI", res.getIntent());
        assertEquals("bar", res.getGrafikTipi());
        assertNotNull(res.getTabloVerisi());
        assertFalse(res.getTabloVerisi().isEmpty());
        assertTrue(res.getCevapMetni().contains("150000"));
        // Ciro veritabaninda gruplanir; tum faturalar cekilmez.
        verify(faturaRepository, never()).findBySirketIdOrderByTarihDesc(any());
    }

    @Test
    void aiSorgula_ciroIntent_tarihAraligiSorguyaGecer() {
        when(faturaRepository.cariBazindaCiroTop(eq(1L), any(), any(), any()))
                .thenReturn(List.of(Map.of("cariAd", "ABC Ltd.", "ciro", BigDecimal.valueOf(150000))));

        LocalDate bugun = LocalDate.now();
        AISorguSonucDTO res = sohbetService.aiSorgula("Bu ay en çok ciro yapan müşterilerimiz kimler?", 1L);

        assertEquals("CIRO_MUSTERI", res.getIntent());
        verify(faturaRepository).cariBazindaCiroTop(eq(1L),
                eq(bugun.withDayOfMonth(1)), eq(bugun.withDayOfMonth(bugun.lengthOfMonth())), any());
    }

    @Test
    void aiSorgula_ciroIntent_tarihAraligiYoksaNullGecer() {
        when(faturaRepository.cariBazindaCiroTop(eq(1L), isNull(), isNull(), any()))
                .thenReturn(List.of(Map.of("cariAd", "ABC Ltd.", "ciro", BigDecimal.valueOf(150000))));

        AISorguSonucDTO res = sohbetService.aiSorgula("En çok ciro yapan müşterilerimiz kimler?", 1L);

        assertEquals("CIRO_MUSTERI", res.getIntent());
        verify(faturaRepository).cariBazindaCiroTop(eq(1L), isNull(), isNull(), any());
    }

    @Test
    void aiSorgula_ciroIntent_enFazla5MusteriDoner() {
        List<Map<String, Object>> cok = new java.util.ArrayList<>();
        for (int i = 1; i <= 9; i++) {
            cok.add(Map.of("cariAd", "M" + i, "ciro", BigDecimal.valueOf(1000 - i)));
        }
        when(faturaRepository.cariBazindaCiroTop(eq(1L), isNull(), isNull(), any())).thenReturn(cok);

        AISorguSonucDTO res = sohbetService.aiSorgula("En çok ciro yapan müşterilerimiz kimler?", 1L);

        assertEquals(5, res.getTabloVerisi().size());
        ArgumentCaptor<org.springframework.data.domain.Pageable> captor =
                ArgumentCaptor.forClass(org.springframework.data.domain.Pageable.class);
        verify(faturaRepository).cariBazindaCiroTop(eq(1L), isNull(), isNull(), captor.capture());
        assertEquals(5, captor.getValue().getPageSize(), "Ciro listesi sunucuda 5 kayitla sinirlandirilmalidir");
    }

    @Test
    void aiSorgula_vadeIntent() {
        CariHesap cari = new CariHesap();
        cari.setId(10L);
        cari.setAd("XYZ A.Ş.");

        Fatura f = new Fatura();
        f.setId(101L);
        f.setTur(Fatura.FaturaTur.SATIS);
        f.setDurum(Fatura.FaturaDurum.KESILDI);
        f.setGenelToplam(BigDecimal.valueOf(50000));
        f.setVadeTarihi(LocalDate.now().plusDays(5));
        f.setCariHesap(cari);

        when(faturaRepository.vadesiAraliktakiFaturalar(eq(1L), any(), any())).thenReturn(List.of(f));

        AISorguSonucDTO res = sohbetService.aiSorgula("Gelecek hafta vadesi gelen ödemeler ve tahsilatlar neler?", 1L);
        assertEquals("VADESI_GELEN", res.getIntent());
        assertEquals("doughnut", res.getGrafikTipi());
        assertTrue(res.getCevapMetni().contains("50000"));
        // 15 gunluk pencere veritabaninda uygulanir.
        verify(faturaRepository).vadesiAraliktakiFaturalar(eq(1L), eq(LocalDate.now()), eq(LocalDate.now().plusDays(15)));
        verify(faturaRepository, never()).findBySirketIdOrderByTarihDesc(any());
    }

    @Test
    void aiSorgula_vadeIntent_vadesiOlmayanFaturaAtlanir() {
        Fatura f = new Fatura();
        f.setId(102L);
        f.setTur(Fatura.FaturaTur.SATIS);
        f.setDurum(Fatura.FaturaDurum.KESILDI);
        f.setGenelToplam(BigDecimal.valueOf(1000));
        f.setTarih(null);
        f.setVadeTarihi(null);

        when(faturaRepository.vadesiAraliktakiFaturalar(eq(1L), any(), any())).thenReturn(List.of(f));

        AISorguSonucDTO res = sohbetService.aiSorgula("Vadesi gelen ödemeler neler?", 1L);

        assertEquals("VADESI_GELEN", res.getIntent());
        assertTrue(res.getTabloVerisi().isEmpty());
    }

    @Test
    void aiSorgula_likiditeIntent() {
        Kasa k = new Kasa();
        k.setAd("Merkez Kasa");
        k.setBakiye(BigDecimal.valueOf(25000));

        Banka b = new Banka();
        b.setAd("Garanti Bankası");
        b.setBakiye(BigDecimal.valueOf(150000));

        when(kasaRepository.findBySirketIdOrderByAd(1L)).thenReturn(List.of(k));
        when(bankaRepository.findBySirketIdOrderByAd(1L)).thenReturn(List.of(b));

        AISorguSonucDTO res = sohbetService.aiSorgula("Kasa ve banka toplam bakiyemiz nedir?", 1L);
        assertEquals("LIKIDITE", res.getIntent());
        assertTrue(res.getCevapMetni().contains("175000"));
    }

    @Test
    void aiSorgula_llmIntent() {
        com.raspel.erp.dto.sistem.AiConfigDTO aiConfig = com.raspel.erp.dto.sistem.AiConfigDTO.builder()
                .provider("OPENAI")
                .model("gpt-4o")
                .durum("AKTIF")
                .aktif(true)
                .build();

        when(aiConfigService.getConfig(1L)).thenReturn(aiConfig);
        when(aiConfigService.getDecryptedKey(1L)).thenReturn("sk-test-key");
        when(llmClientService.sendQuery(eq("OPENAI"), eq("gpt-4o"), eq("sk-test-key"), anyString(), eq("Özel soru")))
                .thenReturn("LLM tarafından üretilen yanıt");

        AISorguSonucDTO res = sohbetService.aiSorgula("Özel soru", 1L);

        assertNotNull(res);
        assertEquals("LLM_YANIT", res.getIntent());
        assertEquals("LLM tarafından üretilen yanıt", res.getCevapMetni());
    }
}
