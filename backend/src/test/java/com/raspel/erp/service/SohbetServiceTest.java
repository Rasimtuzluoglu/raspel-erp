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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SohbetServiceTest {

    @Mock private SohbetMesajRepository sohbetMesajRepository;
    @Mock private SimpMessagingTemplate messagingTemplate;
    @Mock private FaturaRepository faturaRepository;
    @Mock private CariHesapRepository cariHesapRepository;
    @Mock private StokRepository stokRepository;
    @Mock private KasaRepository kasaRepository;
    @Mock private BankaRepository bankaRepository;

    @InjectMocks private SohbetService sohbetService;

    @Test
    void sonMesajlar_returnsReversedList() {
        SohbetMesaj m1 = SohbetMesaj.builder().id(1L).mesaj("Mesaj 1").olusturmaTarihi(LocalDateTime.now()).build();
        SohbetMesaj m2 = SohbetMesaj.builder().id(2L).mesaj("Mesaj 2").olusturmaTarihi(LocalDateTime.now()).build();
        when(sohbetMesajRepository.findTop50BySirketIdOrderByOlusturmaTarihiDesc(1L)).thenReturn(new java.util.ArrayList<>(List.of(m2, m1)));

        List<SohbetMesajDTO> result = sohbetService.sonMesajlar(1L);
        assertEquals(2, result.size());
        assertEquals("Mesaj 1", result.get(0).getMesaj());
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
    void aiSorgula_ciroIntent() {
        CariHesap cari = new CariHesap();
        cari.setId(10L);
        cari.setAd("ABC Ltd.");

        Fatura f = new Fatura();
        f.setId(100L);
        f.setTur(Fatura.FaturaTur.SATIS);
        f.setDurum(Fatura.FaturaDurum.KESILDI);
        f.setGenelToplam(BigDecimal.valueOf(150000));
        f.setCariHesap(cari);

        when(faturaRepository.findBySirketIdOrderByTarihDesc(1L)).thenReturn(List.of(f));

        AISorguSonucDTO res = sohbetService.aiSorgula("Bu ay en çok ciro yapan müşterilerimiz kimler?", 1L);
        assertEquals("CIRO_MUSTERI", res.getIntent());
        assertEquals("bar", res.getGrafikTipi());
        assertNotNull(res.getTabloVerisi());
        assertFalse(res.getTabloVerisi().isEmpty());
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

        when(faturaRepository.findBySirketIdOrderByTarihDesc(1L)).thenReturn(List.of(f));

        AISorguSonucDTO res = sohbetService.aiSorgula("Gelecek hafta vadesi gelen ödemeler ve tahsilatlar neler?", 1L);
        assertEquals("VADESI_GELEN", res.getIntent());
        assertEquals("doughnut", res.getGrafikTipi());
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
}
