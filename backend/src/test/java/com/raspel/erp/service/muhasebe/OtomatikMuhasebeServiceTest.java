package com.raspel.erp.service.muhasebe;

import com.raspel.erp.dto.muhasebe.MuhasebeFisKalemDTO;
import com.raspel.erp.dto.muhasebe.MuhasebeFisiDTO;
import com.raspel.erp.exception.BusinessException;
import com.raspel.erp.exception.ResourceNotFoundException;
import com.raspel.erp.entity.ik.MaasBordro;
import com.raspel.erp.entity.ik.Personel;
import com.raspel.erp.entity.muhasebe.MuhasebeFisi;
import com.raspel.erp.repository.muhasebe.HesapPlaniRepository;
import com.raspel.erp.repository.muhasebe.MuhasebeFisiRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OtomatikMuhasebeServiceTest {

    @Mock private MuhasebeService muhasebeService;
    @Mock private MuhasebeFisiRepository muhasebeFisiRepository;
    @Mock private HesapPlaniRepository hesapPlaniRepository;
    @Mock private com.raspel.erp.repository.envanter.StokMaliyetHareketRepository stokMaliyetHareketRepository;
    @Mock private com.raspel.erp.repository.ticaret.IadeKalemRepository iadeKalemRepository;
    @Mock private com.raspel.erp.repository.muhasebe.MuhasebeFisKalemRepository muhasebeFisKalemRepository;
    @Mock private com.raspel.erp.repository.ticaret.FaturaRepository faturaRepository;
    @Mock private com.raspel.erp.service.sistem.TcmbKurService tcmbKurService;
    @InjectMocks private OtomatikMuhasebeService otomatikMuhasebeService;

    private MaasBordro bordro() {
        Personel p = new Personel();
        p.setAd("Ahmet");
        p.setSoyad("Yilmaz");
        return MaasBordro.builder()
                .id(5L).sirketId(1L)
                .brutMaas(new BigDecimal("30000.00"))
                .kesintiler(new BigDecimal("9000.00"))
                .netMaas(new BigDecimal("21000.00"))
                .yil(2026).ay(7)
                .odemeTarihi(LocalDate.of(2026, 7, 30))
                .personel(p)
                .build();
    }

    @Test
    void bordroIsle_denkFisOlusturur() {
        when(muhasebeFisiRepository.findFirstBySirketIdAndKaynakTipAndKaynakIdAndDurumNot(any(), any(), any(), any()))
                .thenReturn(Optional.empty());
        when(hesapPlaniRepository.findBySirketIdAndKod(anyLong(), any())).thenReturn(Optional.empty());

        otomatikMuhasebeService.bordroIsle(bordro());

        ArgumentCaptor<MuhasebeFisiDTO> captor = ArgumentCaptor.forClass(MuhasebeFisiDTO.class);
        verify(muhasebeService).fisOlustur(captor.capture());
        MuhasebeFisiDTO dto = captor.getValue();
        assertEquals("BORDRO", dto.getKaynakTip());
        assertEquals(5L, dto.getKaynakId());

        BigDecimal borc = dto.getKalemler().stream().map(MuhasebeFisKalemDTO::getBorc).filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal alacak = dto.getKalemler().stream().map(MuhasebeFisKalemDTO::getAlacak).filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(0, borc.compareTo(new BigDecimal("30000.00")));
        assertEquals(0, alacak.compareTo(new BigDecimal("30000.00")));
        // 770 borç, 335 alacak net, 360 alacak kesinti
        assertTrue(dto.getKalemler().stream().anyMatch(k -> "770".equals(k.getHesapKodu())));
        assertTrue(dto.getKalemler().stream().anyMatch(k -> "335".equals(k.getHesapKodu())
                && k.getAlacak().compareTo(new BigDecimal("21000.00")) == 0));
        assertTrue(dto.getKalemler().stream().anyMatch(k -> "360".equals(k.getHesapKodu())
                && k.getAlacak().compareTo(new BigDecimal("9000.00")) == 0));
    }

    @Test
    void bordroIsle_idempotent() {
        when(muhasebeFisiRepository.findFirstBySirketIdAndKaynakTipAndKaynakIdAndDurumNot(any(), any(), any(), any()))
                .thenReturn(Optional.of(new MuhasebeFisi()));

        otomatikMuhasebeService.bordroIsle(bordro());

        verify(muhasebeService, never()).fisOlustur(any());
    }

    /**
     * C5 (fail-closed): fiş oluşturma başarısız olduğunda hata SESSIZCE yutulmaz.
     * Önceki davranış "mahsup yok" diye geçip mali tabloyu eksik bırakıyordu;
     * çağıran transaction'ın geri alınması gerekiyor ki tutarsız muhasebe kaydı
     * oluşmasın.
     */
    @Test
    void bordroIsle_hataDurumundaIslemiDurdurur() {
        when(muhasebeFisiRepository.findFirstBySirketIdAndKaynakTipAndKaynakIdAndDurumNot(any(), any(), any(), any()))
                .thenReturn(Optional.empty());
        when(hesapPlaniRepository.findBySirketIdAndKod(anyLong(), any())).thenReturn(Optional.empty());
        when(muhasebeService.fisOlustur(any())).thenThrow(new RuntimeException("hesap yok"));

        var hata = assertThrows(BusinessException.class,
                () -> otomatikMuhasebeService.bordroIsle(bordro()));
        assertTrue(hata.getMessage().contains("otomatik muhasebe"));
        // Teknik detaj (sinif adi, ic mesaj) yalnizca log'a gider; kullaniciya sizmaz.
        assertFalse(hata.getMessage().contains("hesap yok"));
        assertFalse(hata.getMessage().contains("RuntimeException"));
        // Nedeni korunur: log/izleme icin cause zinciri kaybolmaz.
        assertNotNull(hata.getCause());
        assertEquals("hesap yok", hata.getCause().getMessage());
    }

    /** C5: isletme hatalari olduğu gibi yeniden firlatilir (anlamli mesaj korunur). */
    @Test
    void bordroIsle_isletmeHatasiAynenYenidenFirlatilir() {
        when(muhasebeFisiRepository.findFirstBySirketIdAndKaynakTipAndKaynakIdAndDurumNot(any(), any(), any(), any()))
                .thenReturn(Optional.empty());
        when(hesapPlaniRepository.findBySirketIdAndKod(anyLong(), any())).thenReturn(Optional.empty());
        when(muhasebeService.fisOlustur(any()))
                .thenThrow(new BusinessException("Hesap kodu 770 tanimli degil"));

        var hata = assertThrows(BusinessException.class,
                () -> otomatikMuhasebeService.bordroIsle(bordro()));
        assertEquals("Hesap kodu 770 tanimli degil", hata.getMessage());
    }

    /** C5: ResourceNotFoundException da oldugu gibi gecer. */
    @Test
    void bordroIsle_kayitBulunamadiAynenYenidenFirlatilir() {
        when(muhasebeFisiRepository.findFirstBySirketIdAndKaynakTipAndKaynakIdAndDurumNot(any(), any(), any(), any()))
                .thenReturn(Optional.empty());
        when(hesapPlaniRepository.findBySirketIdAndKod(anyLong(), any())).thenReturn(Optional.empty());
        when(muhasebeService.fisOlustur(any()))
                .thenThrow(new ResourceNotFoundException("Hesap kodu 335 bulunamadi"));

        var hata = assertThrows(ResourceNotFoundException.class,
                () -> otomatikMuhasebeService.bordroIsle(bordro()));
        assertEquals("Hesap kodu 335 bulunamadi", hata.getMessage());
    }

    private com.raspel.erp.entity.ticaret.Fatura fatura(String tur, String ara, String kdv, String toplam) {
        return com.raspel.erp.entity.ticaret.Fatura.builder()
                .id(9L).faturaNumarasi("FTR-9").sirketId(1L)
                .tur(com.raspel.erp.entity.ticaret.Fatura.FaturaTur.valueOf(tur))
                .tarih(LocalDate.of(2026, 7, 15))
                .araToplam(new BigDecimal(ara)).kdv(new BigDecimal(kdv)).genelToplam(new BigDecimal(toplam))
                .build();
    }

    @Test
    void satisFaturaIsle_120Borc600Alacak391Alacak() {
        when(muhasebeFisiRepository.findFirstBySirketIdAndKaynakTipAndKaynakIdAndDurumNot(any(), any(), any(), any()))
                .thenReturn(Optional.empty());
        when(hesapPlaniRepository.findBySirketIdAndKod(anyLong(), any())).thenReturn(Optional.empty());

        otomatikMuhasebeService.satisFaturaIsle(fatura("SATIS", "1000", "200", "1200"));

        ArgumentCaptor<MuhasebeFisiDTO> captor = ArgumentCaptor.forClass(MuhasebeFisiDTO.class);
        verify(muhasebeService).fisOlustur(captor.capture());
        MuhasebeFisiDTO dto = captor.getValue();
        assertEquals("SATIS_FATURA", dto.getKaynakTip());
        assertTrue(dto.getKalemler().stream().anyMatch(k -> "120".equals(k.getHesapKodu())
                && k.getBorc() != null && k.getBorc().compareTo(new BigDecimal("1200")) == 0));
        assertTrue(dto.getKalemler().stream().anyMatch(k -> "600".equals(k.getHesapKodu())
                && k.getAlacak() != null && k.getAlacak().compareTo(new BigDecimal("1000")) == 0));
        assertTrue(dto.getKalemler().stream().anyMatch(k -> "391".equals(k.getHesapKodu())
                && k.getAlacak() != null && k.getAlacak().compareTo(new BigDecimal("200")) == 0));
    }

    @Test
    void satisFaturaIsle_cogsVarsaSmmSatirlariEklenir() {
        when(muhasebeFisiRepository.findFirstBySirketIdAndKaynakTipAndKaynakIdAndDurumNot(any(), any(), any(), any()))
                .thenReturn(Optional.empty());
        when(hesapPlaniRepository.findBySirketIdAndKod(anyLong(), any())).thenReturn(Optional.empty());
        when(stokMaliyetHareketRepository.findByKaynakTipAndKaynakId(eq("FATURA"), anyLong()))
                .thenReturn(java.util.List.of(com.raspel.erp.entity.envanter.StokMaliyetHareket.builder()
                        .tur("CIKIS").toplamMaliyet(new BigDecimal("600")).build()));

        otomatikMuhasebeService.satisFaturaIsle(fatura("SATIS", "1000", "200", "1200"));

        ArgumentCaptor<MuhasebeFisiDTO> captor = ArgumentCaptor.forClass(MuhasebeFisiDTO.class);
        verify(muhasebeService).fisOlustur(captor.capture());
        MuhasebeFisiDTO dto = captor.getValue();
        // Satılan malın maliyeti: Borç 621 / Alacak 153.
        assertTrue(dto.getKalemler().stream().anyMatch(k -> "621".equals(k.getHesapKodu())
                && k.getBorc() != null && k.getBorc().compareTo(new BigDecimal("600")) == 0));
        assertTrue(dto.getKalemler().stream().anyMatch(k -> "153".equals(k.getHesapKodu())
                && k.getAlacak() != null && k.getAlacak().compareTo(new BigDecimal("600")) == 0));
    }

    @Test
    void iadeIsle_satisIadesi610Borc391Borc120Alacak() {
        when(muhasebeFisiRepository.findFirstBySirketIdAndKaynakTipAndKaynakIdAndDurumNot(any(), any(), any(), any()))
                .thenReturn(Optional.empty());
        when(hesapPlaniRepository.findBySirketIdAndKod(anyLong(), any())).thenReturn(Optional.empty());
        when(iadeKalemRepository.findByIadeId(3L)).thenReturn(java.util.List.of(
                com.raspel.erp.entity.ticaret.IadeKalem.builder().iadeId(3L)
                        .miktar(BigDecimal.ONE).birimFiyat(new BigDecimal("120"))
                        .kdvOrani(new BigDecimal("20")).build()));
        when(stokMaliyetHareketRepository.findByKaynakTipAndKaynakId(eq("IADE"), anyLong()))
                .thenReturn(java.util.List.of());

        otomatikMuhasebeService.iadeIsle(com.raspel.erp.entity.ticaret.Iade.builder()
                .id(3L).sirketId(1L).tur("SATIS").tarih(LocalDate.of(2026, 7, 20))
                .tutar(new BigDecimal("120")).build());

        ArgumentCaptor<MuhasebeFisiDTO> captor = ArgumentCaptor.forClass(MuhasebeFisiDTO.class);
        verify(muhasebeService).fisOlustur(captor.capture());
        MuhasebeFisiDTO dto = captor.getValue();
        assertEquals("IADE", dto.getKaynakTip());
        // KDV dahil 120 TL iade: matrah 100 (610 borç), KDV 20 (391 borç), alıcılar 120 alacak.
        assertTrue(dto.getKalemler().stream().anyMatch(k -> "610".equals(k.getHesapKodu())
                && k.getBorc() != null && k.getBorc().compareTo(new BigDecimal("100")) == 0));
        assertTrue(dto.getKalemler().stream().anyMatch(k -> "391".equals(k.getHesapKodu())
                && k.getBorc() != null && k.getBorc().compareTo(new BigDecimal("20")) == 0));
        assertTrue(dto.getKalemler().stream().anyMatch(k -> "120".equals(k.getHesapKodu())
                && k.getAlacak() != null && k.getAlacak().compareTo(new BigDecimal("120")) == 0));
    }

    @Test
    void cekSenetTahsilIsle_kasaSecilirseFisOlusur() {
        when(muhasebeFisiRepository.findFirstBySirketIdAndKaynakTipAndKaynakIdAndDurumNot(any(), any(), any(), any()))
                .thenReturn(Optional.empty());
        when(hesapPlaniRepository.findBySirketIdAndKod(anyLong(), any())).thenReturn(Optional.empty());

        otomatikMuhasebeService.cekSenetTahsilIsle(com.raspel.erp.entity.finans.CekSenet.builder()
                .id(4L).sirketId(1L).cekNo("123").tutar(new BigDecimal("500")).build(), 2L, null);

        ArgumentCaptor<MuhasebeFisiDTO> captor = ArgumentCaptor.forClass(MuhasebeFisiDTO.class);
        verify(muhasebeService).fisOlustur(captor.capture());
        MuhasebeFisiDTO dto = captor.getValue();
        assertEquals("CEK_SENET", dto.getKaynakTip());
        assertTrue(dto.getKalemler().stream().anyMatch(k -> "100".equals(k.getHesapKodu())
                && k.getBorc() != null && k.getBorc().compareTo(new BigDecimal("500")) == 0));
        assertTrue(dto.getKalemler().stream().anyMatch(k -> "120".equals(k.getHesapKodu())
                && k.getAlacak() != null && k.getAlacak().compareTo(new BigDecimal("500")) == 0));
    }

    @Test
    void cekSenetTahsilIsle_hesapSecilmemisseFisOlusmaz() {
        otomatikMuhasebeService.cekSenetTahsilIsle(com.raspel.erp.entity.finans.CekSenet.builder()
                .id(4L).sirketId(1L).tutar(new BigDecimal("500")).build(), null, null);
        verify(muhasebeService, never()).fisOlustur(any());
    }

    @Test
    void yilSonuKapanisFisi_gelirGider690aAktarilirVeKarDevredilir() {
        when(muhasebeFisiRepository.findFirstBySirketIdAndKaynakTipAndKaynakIdAndDurumNot(any(), any(), any(), any()))
                .thenReturn(Optional.empty());
        when(hesapPlaniRepository.findBySirketIdAndKod(anyLong(), any())).thenReturn(Optional.empty());
        when(muhasebeFisKalemRepository.aktifKalemler(eq(1L), any(), any())).thenReturn(java.util.List.of(
                com.raspel.erp.entity.muhasebe.MuhasebeFisKalem.builder()
                        .hesapKodu("600").alacak(new BigDecimal("1000")).build(),
                com.raspel.erp.entity.muhasebe.MuhasebeFisKalem.builder()
                        .hesapKodu("621").borc(new BigDecimal("400")).build()));

        otomatikMuhasebeService.yilSonuKapanisFisi(1L, 2026);

        ArgumentCaptor<MuhasebeFisiDTO> captor = ArgumentCaptor.forClass(MuhasebeFisiDTO.class);
        verify(muhasebeService).fisOlustur(captor.capture());
        MuhasebeFisiDTO dto = captor.getValue();
        assertEquals("YIL_SONU_KAPANIS", dto.getKaynakTip());
        // Gelir hesabı borçlandırılır (600 borç 1000), gider hesabı alacaklanır (621 alacak 400).
        assertTrue(dto.getKalemler().stream().anyMatch(k -> "600".equals(k.getHesapKodu())
                && k.getBorc() != null && k.getBorc().compareTo(new BigDecimal("1000")) == 0));
        assertTrue(dto.getKalemler().stream().anyMatch(k -> "621".equals(k.getHesapKodu())
                && k.getAlacak() != null && k.getAlacak().compareTo(new BigDecimal("400")) == 0));
        // Kâr 600 TL: 690 borç / 570 alacak.
        assertTrue(dto.getKalemler().stream().anyMatch(k -> "570".equals(k.getHesapKodu())
                && k.getAlacak() != null && k.getAlacak().compareTo(new BigDecimal("600")) == 0));
    }

    @Test
    void yilSonuKapanisFisi_kapaliysaMukerrerFisOlusmaz() {
        when(muhasebeFisiRepository.findFirstBySirketIdAndKaynakTipAndKaynakIdAndDurumNot(any(), any(), any(), any()))
                .thenReturn(Optional.of(new MuhasebeFisi()));
        otomatikMuhasebeService.yilSonuKapanisFisi(1L, 2026);
        verify(muhasebeService, never()).fisOlustur(any());
    }

    @Test
    void fxDegerleme_satisFaturasiIcinKambiyoKariIslenir() {
        when(muhasebeFisiRepository.findFirstBySirketIdAndKaynakTipAndKaynakIdAndDurumNot(any(), any(), any(), any()))
                .thenReturn(Optional.empty());
        when(hesapPlaniRepository.findBySirketIdAndKod(anyLong(), any())).thenReturn(Optional.empty());
        when(faturaRepository.findBySirketIdAndDurumAndParaBirimiNotAndKalanTutarGreaterThan(
                eq(1L), any(), eq("TRY"), any())).thenReturn(java.util.List.of(
                com.raspel.erp.entity.ticaret.Fatura.builder()
                        .id(7L).faturaNumarasi("FTR-USD-1").sirketId(1L)
                        .tur(com.raspel.erp.entity.ticaret.Fatura.FaturaTur.SATIS)
                        .durum(com.raspel.erp.entity.ticaret.Fatura.FaturaDurum.KESILDI)
                        .paraBirimi("USD").kur(new BigDecimal("30.000000"))
                        .kalanTutar(new BigDecimal("3000.00")).build()));
        // Güncel kur 32: 100 USD kalan x 2 TL fark = 200 TL kambiyo kârı.
        when(tcmbKurService.cevir(any(), eq("USD"), eq("TRY"))).thenReturn(new BigDecimal("32.000000"));

        otomatikMuhasebeService.fxDegerlemeFisi(1L, LocalDate.of(2026, 9, 30));

        ArgumentCaptor<MuhasebeFisiDTO> captor = ArgumentCaptor.forClass(MuhasebeFisiDTO.class);
        verify(muhasebeService).fisOlustur(captor.capture());
        MuhasebeFisiDTO dto = captor.getValue();
        assertEquals("FX_DEGERLEME", dto.getKaynakTip());
        assertTrue(dto.getKalemler().stream().anyMatch(k -> "120".equals(k.getHesapKodu())
                && k.getBorc() != null && k.getBorc().compareTo(new BigDecimal("200.00")) == 0));
        assertTrue(dto.getKalemler().stream().anyMatch(k -> "646".equals(k.getHesapKodu())
                && k.getAlacak() != null && k.getAlacak().compareTo(new BigDecimal("200.00")) == 0));
    }

    @Test
    void alisFaturaIsle_153Ve191Borc320Alacak() {
        when(muhasebeFisiRepository.findFirstBySirketIdAndKaynakTipAndKaynakIdAndDurumNot(any(), any(), any(), any()))
                .thenReturn(Optional.empty());
        when(hesapPlaniRepository.findBySirketIdAndKod(anyLong(), any())).thenReturn(Optional.empty());

        otomatikMuhasebeService.alisFaturaIsle(fatura("ALIS", "1000", "200", "1200"));

        ArgumentCaptor<MuhasebeFisiDTO> captor = ArgumentCaptor.forClass(MuhasebeFisiDTO.class);
        verify(muhasebeService).fisOlustur(captor.capture());
        MuhasebeFisiDTO dto = captor.getValue();
        assertEquals("ALIS_FATURA", dto.getKaynakTip());
        assertTrue(dto.getKalemler().stream().anyMatch(k -> "153".equals(k.getHesapKodu()) && k.getBorc() != null));
        assertTrue(dto.getKalemler().stream().anyMatch(k -> "191".equals(k.getHesapKodu()) && k.getBorc() != null));
        assertTrue(dto.getKalemler().stream().anyMatch(k -> "320".equals(k.getHesapKodu())
                && k.getAlacak() != null && k.getAlacak().compareTo(new BigDecimal("1200")) == 0));
    }

    @Test
    void tahsilatIsle_kasaSecilirse100Borc() {
        when(muhasebeFisiRepository.findFirstBySirketIdAndKaynakTipAndKaynakIdAndDurumNot(any(), any(), any(), any()))
                .thenReturn(Optional.empty());
        when(hesapPlaniRepository.findBySirketIdAndKod(anyLong(), any())).thenReturn(Optional.empty());

        otomatikMuhasebeService.tahsilatIsle(1L, 77L, new BigDecimal("500"), LocalDate.of(2026, 7, 20),
                3L, null, "Acme");

        ArgumentCaptor<MuhasebeFisiDTO> captor = ArgumentCaptor.forClass(MuhasebeFisiDTO.class);
        verify(muhasebeService).fisOlustur(captor.capture());
        assertTrue(captor.getValue().getKalemler().stream().anyMatch(k -> "100".equals(k.getHesapKodu())));
        assertTrue(captor.getValue().getKalemler().stream().anyMatch(k -> "120".equals(k.getHesapKodu())));
    }

    @Test
    void tahsilatIsle_nakitHesapYoksaFisUretmez() {
        otomatikMuhasebeService.tahsilatIsle(1L, 78L, new BigDecimal("500"), LocalDate.of(2026, 7, 20),
                null, null, "Acme");

        verify(muhasebeService, never()).fisOlustur(any());
    }
}
