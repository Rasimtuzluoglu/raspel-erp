package com.raspel.erp.service.envanter;

import com.raspel.erp.dto.envanter.ReceteDTO;
import com.raspel.erp.dto.envanter.UretimEmriDTO;
import com.raspel.erp.dto.envanter.UretimIhtiyacDTO;
import com.raspel.erp.dto.envanter.UretimTamamlaIstek;
import com.raspel.erp.entity.envanter.Recete;
import com.raspel.erp.entity.envanter.ReceteKalem;
import com.raspel.erp.entity.envanter.Stok;
import com.raspel.erp.entity.envanter.UretimEmri;
import com.raspel.erp.exception.BusinessException;
import com.raspel.erp.exception.ResourceNotFoundException;
import com.raspel.erp.repository.envanter.ReceteKalemRepository;
import com.raspel.erp.repository.envanter.ReceteRepository;
import com.raspel.erp.repository.envanter.StokHareketRepository;
import com.raspel.erp.repository.envanter.StokRepository;
import com.raspel.erp.repository.envanter.UretimEmriRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UretimServiceTest {

    @Mock private ReceteRepository receteRepository;
    @Mock private ReceteKalemRepository receteKalemRepository;
    @Mock private UretimEmriRepository uretimEmriRepository;
    @Mock private com.raspel.erp.repository.envanter.UretimEmriLogRepository uretimEmriLogRepository;
    @Mock private StokRepository stokRepository;
    @Mock private StokHareketRepository stokHareketRepository;
    @Mock private com.raspel.erp.repository.ticaret.SiparisRepository siparisRepository;
    @Mock private com.raspel.erp.repository.ticaret.SiparisKalemRepository siparisKalemRepository;
    @Mock private com.raspel.erp.service.ticaret.SatinalmaTalepService satinalmaTalepService;
    @Mock private com.raspel.erp.service.sube.DepoStokService depoStokService;
    @Mock private com.raspel.erp.service.envanter.StokSeriService stokSeriService;
    @Mock private com.raspel.erp.service.envanter.MaliyetService maliyetService;
    @Mock private com.raspel.erp.config.TenantChecker tenantChecker;
    @InjectMocks private UretimService uretimService;

    private Stok stok(Long id, String ad, String miktar) {
        Stok s = new Stok();
        s.setId(id);
        s.setAd(ad);
        s.setMiktar(new BigDecimal(miktar));
        return s;
    }

    @Test
    void receteOlustur_bosAdHataVerir() {
        assertThrows(BusinessException.class,
                () -> uretimService.receteOlustur(ReceteDTO.builder().ad(" ").urunId(1L).build(), 1L));
    }

    @Test
    void uretimEmriTamamla_hammaddeleriTuketirVeMamulUretir() {
        UretimEmri emri = UretimEmri.builder().id(1L).sirketId(1L).urunId(10L)
                .miktar(new BigDecimal("2")).durum("TASLAK").build();
        Recete recete = Recete.builder().id(5L).sirketId(1L).ad("Masa").urunId(10L).build();
        ReceteKalem kalem = ReceteKalem.builder().id(1L).receteId(5L).hammaddeId(1L).miktar(new BigDecimal("3")).build();
        Stok hammadde = stok(1L, "MDF", "10");
        Stok mamul = stok(10L, "Masa", "0");

        when(uretimEmriRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(emri));
        when(receteRepository.findFirstBySirketIdAndUrunIdAndAktifTrueOrderByRevizyonDesc(1L, 10L)).thenReturn(Optional.of(recete));
        when(receteKalemRepository.findByReceteId(5L)).thenReturn(List.of(kalem));
        when(stokRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(hammadde));
        when(stokRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(mamul));

        uretimService.emirTamamla(1L, 1L, null, null);

        assertEquals(0, hammadde.getMiktar().compareTo(new BigDecimal("4")));  // 10 - 3*2
        assertEquals(0, mamul.getMiktar().compareTo(new BigDecimal("2")));
        assertEquals("TAMAMLANDI", emri.getDurum());
        verify(stokHareketRepository, times(2)).save(any());
    }

    @Test
    void uretimEmriTamamla_yetersizHammaddeHataVerir() {
        UretimEmri emri = UretimEmri.builder().id(1L).sirketId(1L).urunId(10L)
                .miktar(new BigDecimal("5")).durum("TASLAK").build();
        Recete recete = Recete.builder().id(5L).sirketId(1L).ad("Masa").urunId(10L).build();
        ReceteKalem kalem = ReceteKalem.builder().id(1L).receteId(5L).hammaddeId(1L).miktar(new BigDecimal("3")).build();
        Stok hammadde = stok(1L, "MDF", "10");

        when(uretimEmriRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(emri));
        when(receteRepository.findFirstBySirketIdAndUrunIdAndAktifTrueOrderByRevizyonDesc(1L, 10L)).thenReturn(Optional.of(recete));
        when(receteKalemRepository.findByReceteId(5L)).thenReturn(List.of(kalem));
        when(stokRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(hammadde));

        assertThrows(BusinessException.class, () -> uretimService.emirTamamla(1L, 1L, null, null));
    }

    @Test
    void uretimEmriOlustur_gecersizMiktarHataVerir() {
        assertThrows(BusinessException.class,
                () -> uretimService.uretimEmriOlustur(UretimEmriDTO.builder().urunId(1L).miktar(BigDecimal.ZERO).build(), 1L));
    }

    @Test
    void emirBaslat_taslakEmriUretimdeYaparVeLogYazar() {
        UretimEmri emri = UretimEmri.builder().id(1L).sirketId(1L).urunId(10L)
                .miktar(BigDecimal.ONE).durum("TASLAK").build();
        when(uretimEmriRepository.findById(1L)).thenReturn(Optional.of(emri));
        when(uretimEmriRepository.save(any())).thenAnswer((inv) -> inv.getArgument(0));

        UretimEmriDTO dto = uretimService.emirBaslat(1L, 1L, 5L);

        assertEquals("URETIMDE", dto.getDurum());
        assertNotNull(emri.getBaslamaTarihi());
        verify(uretimEmriLogRepository).save(any());
    }

    @Test
    void emirBaslat_farkliSirketHataVerir() {
        UretimEmri emri = UretimEmri.builder().id(1L).sirketId(2L).urunId(10L)
                .miktar(BigDecimal.ONE).durum("TASLAK").build();
        when(uretimEmriRepository.findById(1L)).thenReturn(Optional.of(emri));

        assertThrows(BusinessException.class, () -> uretimService.emirBaslat(1L, 1L, 5L));
    }

    /**
     * C3: emir tamamlamada emri KİLİTLİ okumak zorunlu. Kilit olmadan iki
     * eşzamanlı istek ikisi de geçer, hammadde iki kez tüketilir ve mamul
     * iki kez üretilir.
     */
    @Test
    void emirTamamla_emriKilitliOkur() {
        UretimEmri emri = UretimEmri.builder().id(1L).sirketId(1L).urunId(10L)
                .miktar(BigDecimal.ONE).durum("URETIMDE").build();
        Recete recete = Recete.builder().id(5L).sirketId(1L).ad("Masa").urunId(10L).build();
        ReceteKalem kalem = ReceteKalem.builder().id(1L).receteId(5L).hammaddeId(1L).miktar(BigDecimal.ONE).build();
        Stok hammadde = stok(1L, "MDF", "10");
        Stok mamul = stok(10L, "Masa", "0");

        when(uretimEmriRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(emri));
        when(receteRepository.findFirstBySirketIdAndUrunIdAndAktifTrueOrderByRevizyonDesc(1L, 10L)).thenReturn(Optional.of(recete));
        when(receteKalemRepository.findByReceteId(5L)).thenReturn(List.of(kalem));
        when(stokRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(hammadde));
        when(stokRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(mamul));

        uretimService.emirTamamla(1L, 1L, null, null);

        verify(uretimEmriRepository).findByIdForUpdate(1L);
        verify(uretimEmriRepository, never()).findById(1L);
        // C1/C3: emrin tenant'ı doğrulanır.
        verify(tenantChecker).check(1L, "Uretim Emri");
    }

    /** C3: başka şirketin emri tamamlanamaz. */
    @Test
    void emirTamamla_baskaSirketEmriReddedilir() {
        UretimEmri emri = UretimEmri.builder().id(1L).sirketId(2L).urunId(10L)
                .miktar(BigDecimal.ONE).durum("URETIMDE").build();
        when(uretimEmriRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(emri));
        doThrow(new ResourceNotFoundException("Uretim Emri")).when(tenantChecker).check(2L, "Uretim Emri");

        assertThrows(ResourceNotFoundException.class,
                () -> uretimService.emirTamamla(1L, 1L, null, null));
        // Hiçbir stok hareketi yazılmamalı.
        verify(stokHareketRepository, never()).save(any());
    }

    @Test
    void emirTamamla_kismiUretimVeFire() {
        UretimEmri emri = UretimEmri.builder().id(1L).sirketId(1L).urunId(10L)
                .miktar(new BigDecimal("2")).durum("URETIMDE").build();
        Recete recete = Recete.builder().id(5L).sirketId(1L).ad("Masa").urunId(10L).build();
        ReceteKalem kalem = ReceteKalem.builder().id(1L).receteId(5L).hammaddeId(1L).miktar(new BigDecimal("3")).build();
        Stok hammadde = stok(1L, "MDF", "100");
        Stok mamul = stok(10L, "Masa", "0");

        when(uretimEmriRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(emri));
        when(receteRepository.findFirstBySirketIdAndUrunIdAndAktifTrueOrderByRevizyonDesc(1L, 10L)).thenReturn(Optional.of(recete));
        when(receteKalemRepository.findByReceteId(5L)).thenReturn(List.of(kalem));
        when(stokRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(hammadde));
        when(stokRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(mamul));

        UretimTamamlaIstek istek = UretimTamamlaIstek.builder()
                .uretilenMiktar(new BigDecimal("2"))
                .fireMiktar(new BigDecimal("1"))
                .iscilikMaliyeti(new BigDecimal("50"))
                .build();
        UretimEmriDTO dto = uretimService.emirTamamla(1L, 1L, istek, null);

        assertEquals(0, hammadde.getMiktar().compareTo(new BigDecimal("91"))); // 3 * (2+1)
        assertEquals(0, mamul.getMiktar().compareTo(new BigDecimal("2")));     // sadece Ã¼retilen
        assertEquals("TAMAMLANDI", dto.getDurum());
        assertEquals(0, dto.getFireMiktar().compareTo(BigDecimal.ONE));
        assertTrue(dto.getToplamMaliyet().compareTo(new BigDecimal("50")) >= 0);
    }

    @Test
    void ihtiyacAnalizi_eksikHesaplar() {
        Recete recete = Recete.builder().id(5L).sirketId(1L).ad("Masa").urunId(10L).build();
        ReceteKalem kalem = ReceteKalem.builder().id(1L).receteId(5L).hammaddeId(1L).miktar(new BigDecimal("3")).build();
        Stok hammadde = stok(1L, "MDF", "2");

        when(receteRepository.findFirstBySirketIdAndUrunIdAndAktifTrueOrderByRevizyonDesc(1L, 10L)).thenReturn(Optional.of(recete));
        when(receteKalemRepository.findByReceteId(5L)).thenReturn(List.of(kalem));
        when(stokRepository.findById(1L)).thenReturn(Optional.of(hammadde));

        UretimIhtiyacDTO ihtiyac = uretimService.ihtiyacAnalizi(10L, new BigDecimal("2"), 1L);

        assertFalse(ihtiyac.isYeterli());
        assertEquals(0, ihtiyac.getKalemler().get(0).getEksik().compareTo(new BigDecimal("4"))); // gerekli 6, mevcut 2
    }

    /** Özellik: reçete baz miktarı (ör. "2 litre kola") üzerinden ölçekleme. */
    @Test
    void ihtiyacAnalizi_bazMiktarOlarakOlcekler() {
        Recete recete = Recete.builder().id(5L).sirketId(1L).ad("Kola").urunId(10L)
                .bazMiktar(new BigDecimal("2")).build();
        ReceteKalem kalem = ReceteKalem.builder().id(1L).receteId(5L).hammaddeId(1L).miktar(new BigDecimal("3")).build();
        Stok hammadde = stok(1L, "Seker", "100");

        when(receteRepository.findFirstBySirketIdAndUrunIdAndAktifTrueOrderByRevizyonDesc(1L, 10L)).thenReturn(Optional.of(recete));
        when(receteKalemRepository.findByReceteId(5L)).thenReturn(List.of(kalem));
        when(stokRepository.findById(1L)).thenReturn(Optional.of(hammadde));

        // Baz 2 birim icin kalem 3 -> 10 birim icin 10/2 * 3 = 15
        UretimIhtiyacDTO ihtiyac = uretimService.ihtiyacAnalizi(10L, new BigDecimal("10"), 1L);
        assertEquals(0, ihtiyac.getKalemler().get(0).getGerekli().compareTo(new BigDecimal("15")));
    }

    @Test
    void emirTamamla_bazMiktarOlarakOlcekler() {
        UretimEmri emri = UretimEmri.builder().id(1L).sirketId(1L).urunId(10L)
                .miktar(new BigDecimal("4")).durum("URETIMDE").build();
        Recete recete = Recete.builder().id(5L).sirketId(1L).ad("Kola").urunId(10L)
                .bazMiktar(new BigDecimal("2")).build();
        ReceteKalem kalem = ReceteKalem.builder().id(1L).receteId(5L).hammaddeId(1L).miktar(new BigDecimal("3")).build();
        Stok hammadde = stok(1L, "Seker", "100");
        Stok mamul = stok(10L, "Kola", "0");

        when(uretimEmriRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(emri));
        when(receteRepository.findFirstBySirketIdAndUrunIdAndAktifTrueOrderByRevizyonDesc(1L, 10L)).thenReturn(Optional.of(recete));
        when(receteKalemRepository.findByReceteId(5L)).thenReturn(List.of(kalem));
        when(stokRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(hammadde));
        when(stokRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(mamul));

        uretimService.emirTamamla(1L, 1L,
                UretimTamamlaIstek.builder().uretilenMiktar(new BigDecimal("4")).build(), null);

        // Baz 2 -> 4/2 = 2 katsayi; tuketim 3 * 2 = 6; mevcut 100 -> 94
        assertEquals(0, hammadde.getMiktar().compareTo(new BigDecimal("94")));
    }
}
