package com.raspel.erp.service.ik;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.raspel.erp.config.TenantChecker;
import com.raspel.erp.dto.ik.BordroHesaplamaDTO;
import com.raspel.erp.entity.ik.BordroAyar;
import com.raspel.erp.exception.BusinessException;
import com.raspel.erp.repository.ik.BordroAyarRepository;
import com.raspel.erp.repository.ik.MaasBordroRepository;
import com.raspel.erp.repository.ik.PersonelRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BordroHesaplamaServiceTest {

    @Mock private BordroAyarRepository bordroAyarRepository;
    @Mock private PersonelRepository personelRepository;
    @Mock private MaasBordroRepository maasBordroRepository;
    @Mock private TenantChecker tenantChecker;

    private BordroHesaplamaService servis;

    private BordroAyar ayar() {
        return BordroAyar.builder()
                .id(1L).sirketId(1L).yil(2026)
                .asgariUcret(new BigDecimal("20000"))
                .sgkIsciOrani(new BigDecimal("14"))
                .issizlikIsciOrani(new BigDecimal("1"))
                .sgkIsverenOrani(new BigDecimal("20.50"))
                .issizlikIsverenOrani(new BigDecimal("2"))
                .damgaOrani(new BigDecimal("0.759"))
                .gelirVergisiDilimleri("[{\"limit\":158000,\"oran\":15},{\"limit\":null,\"oran\":20}]")
                .build();
    }

    @BeforeEach
    void kur() {
        servis = new BordroHesaplamaService(bordroAyarRepository, personelRepository,
                maasBordroRepository, tenantChecker, new ObjectMapper());
    }

    // --- REDTEAM (Wave 2.3): ayar kaydinda dogrulama eksikti ---

    private BordroAyar ayarDeger(BigDecimal sgk, String dilimler) {
        return BordroAyar.builder()
                .yil(2026).asgariUcret(new BigDecimal("20000"))
                .sgkIsciOrani(sgk).damgaOrani(new BigDecimal("0.759"))
                .gelirVergisiDilimleri(dilimler)
                .build();
    }

    @Test
    void ayarKaydet_negatifSgkOraniniReddeder() {
        var hata = assertThrows(BusinessException.class, () ->
                servis.ayarKaydet(1L, ayarDeger(new BigDecimal("-14"), null)));
        assertTrue(hata.getMessage().contains("0 ile 100"));
    }

    @Test
    void ayarKaydet_yuzdenBuyukOraniReddeder() {
        var hata = assertThrows(BusinessException.class, () ->
                servis.ayarKaydet(1L, ayarDeger(new BigDecimal("140"), null)));
        assertTrue(hata.getMessage().contains("0 ile 100"));
    }

    @Test
    void ayarKaydet_negatifDamgaOraniniReddeder() {
        var ayar = BordroAyar.builder()
                .yil(2026).asgariUcret(new BigDecimal("20000"))
                .sgkIsciOrani(new BigDecimal("14"))
                .damgaOrani(new BigDecimal("-0.759"))
                .build();
        var hata = assertThrows(BusinessException.class, () -> servis.ayarKaydet(1L, ayar));
        assertTrue(hata.getMessage().contains("Damga"));
    }

    /** Bozuk JSON sessizce herkese %15 uyguluyordu; kayıt anında reddedilmeli. */
    @Test
    void ayarKaydet_bozukDilimJsoniniReddeder() {
        var hata = assertThrows(BusinessException.class, () ->
                servis.ayarKaydet(1L, ayarDeger(new BigDecimal("14"), "{bozuk json")));
        assertTrue(hata.getMessage().contains("JSON"));
    }

    @Test
    void ayarKaydet_azalanDilimLimitleriniReddeder() {
        var hata = assertThrows(BusinessException.class, () ->
                servis.ayarKaydet(1L, ayarDeger(new BigDecimal("14"),
                        "[{\"limit\":300000,\"oran\":15},{\"limit\":158000,\"oran\":20}]")));
        assertTrue(hata.getMessage().contains("küçükten büyüğe"));
    }

    @Test
    void ayarKaydet_limitSadeceSonDilimdeBosOlabilir() {
        var hata = assertThrows(BusinessException.class, () ->
                servis.ayarKaydet(1L, ayarDeger(new BigDecimal("14"),
                        "[{\"limit\":null,\"oran\":15},{\"limit\":158000,\"oran\":20}]")));
        assertTrue(hata.getMessage().contains("son dilim"));
    }

    @Test
    void ayarKaydet_dilimOraniYuzdenBuyukseReddeder() {
        var hata = assertThrows(BusinessException.class, () ->
                servis.ayarKaydet(1L, ayarDeger(new BigDecimal("14"),
                        "[{\"limit\":158000,\"oran\":150}]")));
        assertTrue(hata.getMessage().contains("0 ile 100"));
    }

    @Test
    void ayarKaydet_gecerliAyarKaydedilir() {
        when(bordroAyarRepository.findBySirketIdAndYil(1L, 2026)).thenReturn(Optional.empty());
        when(bordroAyarRepository.save(any(BordroAyar.class))).thenAnswer(i -> i.getArgument(0));

        var sonuc = servis.ayarKaydet(1L, ayarDeger(new BigDecimal("14"),
                "[{\"limit\":158000,\"oran\":15},{\"limit\":null,\"oran\":20}]"));

        assertEquals(0, sonuc.getSgkIsciOrani().compareTo(new BigDecimal("14")));
    }

    @Test
    void hesapla_asgariUcretliBrutteGelirVergisiSifirCikar() {
        when(bordroAyarRepository.findBySirketIdAndYil(1L, 2026)).thenReturn(Optional.of(ayar()));

        var sonuc = servis.hesapla(BordroHesaplamaDTO.builder()
                .yil(2026).brutMaas(new BigDecimal("40000")).build(), 1L);

        // SGK işçi: 40.000 x %15 = 6.000; GV matrahı: 40.000 - 6.000 - 17.000 = 17.000
        assertEquals(0, sonuc.getSgkIsciKesintisi().compareTo(new BigDecimal("6000.00")));
        assertEquals(0, sonuc.getGelirVergisiMatrahi().compareTo(new BigDecimal("17000.00")));
        // Asgari ücret istisnası nedeniyle gelir vergisi 0.
        assertEquals(0, sonuc.getGelirVergisi().compareTo(new BigDecimal("0.00")));
        // Damga: (40.000 - 20.000) x %0,759 = 151,80
        assertEquals(0, sonuc.getDamgaVergisi().compareTo(new BigDecimal("151.80")));
        assertEquals(0, sonuc.getNetMaas().compareTo(new BigDecimal("33848.20")));
        // İşveren maliyeti: 40.000 + %22,5 = 49.000
        assertEquals(0, sonuc.getIsverenMaliyeti().compareTo(new BigDecimal("49000.00")));
    }

    @Test
    void hesapla_asgariUcretUstuBrutteVergiHesaplanir() {
        when(bordroAyarRepository.findBySirketIdAndYil(1L, 2026)).thenReturn(Optional.of(ayar()));

        var sonuc = servis.hesapla(BordroHesaplamaDTO.builder()
                .yil(2026).brutMaas(new BigDecimal("60000")).build(), 1L);

        // SGK: 9.000; matrah: 60.000 - 9.000 - 17.000 = 34.000
        assertEquals(0, sonuc.getSgkIsciKesintisi().compareTo(new BigDecimal("9000.00")));
        assertEquals(0, sonuc.getGelirVergisiMatrahi().compareTo(new BigDecimal("34000.00")));
        // GV: 34.000 x %15 = 5.100 - asgari ücret istisnası 2.550 = 2.550
        assertEquals(0, sonuc.getGelirVergisi().compareTo(new BigDecimal("2550.00")));
        // Damga: 40.000 x %0,759 = 303,60
        assertEquals(0, sonuc.getDamgaVergisi().compareTo(new BigDecimal("303.60")));
        assertEquals(0, sonuc.getToplamKesinti().compareTo(new BigDecimal("11853.60")));
        assertEquals(0, sonuc.getNetMaas().compareTo(new BigDecimal("48146.40")));
    }

    @Test
    void hesapla_kumulatifMatrahDilimAsimindaUstDilimUygulanir() {
        when(bordroAyarRepository.findBySirketIdAndYil(1L, 2026)).thenReturn(Optional.of(ayar()));

        // Kümülatif 150.000; bu ay matrah 20.000 -> 8.000'i %15, 12.000'i %20 diliminde.
        var sonuc = servis.hesapla(BordroHesaplamaDTO.builder()
                .yil(2026).brutMaas(new BigDecimal("43000")).kumulatifMatrah(new BigDecimal("150000"))
                .build(), 1L);

        // GV matrahı: 43.000 - 6.450 - 17.000 = 19.550
        assertEquals(0, sonuc.getGelirVergisiMatrahi().compareTo(new BigDecimal("19550.00")));
        // Dilim farkı: (158.000-150.000)x15% + (169.550-158.000)x20% = 1.200 + 2.310 = 3.510
        // Asgari ücret istisnası da kümülatif dilimde: (158.000-150.000)x15% + 9.000x20% = 3.000
        // Ödenecek GV: 3.510 - 3.000 = 510.
        assertEquals(0, sonuc.getGelirVergisi().compareTo(new BigDecimal("510.00")));
    }

    @Test
    void hesapla_kumulatifVerilmezseOncekiAylardanToplar() {
        when(bordroAyarRepository.findBySirketIdAndYil(1L, 2026)).thenReturn(Optional.of(ayar()));
        when(personelRepository.findById(5L)).thenReturn(Optional.of(
                com.raspel.erp.entity.ik.Personel.builder().id(5L).sirketId(1L).ad("A").soyad("B").build()));
        when(maasBordroRepository.kumulatifMatrah(1L, 5L, 2026, 8)).thenReturn(new BigDecimal("100000"));

        var sonuc = servis.hesapla(BordroHesaplamaDTO.builder()
                .personelId(5L).yil(2026).ay(8).brutMaas(new BigDecimal("43000")).build(), 1L);

        assertEquals(0, sonuc.getKumulatifMatrah().compareTo(new BigDecimal("100000")));
        verify(maasBordroRepository).kumulatifMatrah(1L, 5L, 2026, 8);
    }

    @Test
    void topluUret_kumulatifMatrahiKullanirVeKaydeder() {
        when(bordroAyarRepository.findBySirketIdAndYil(1L, 2026)).thenReturn(Optional.of(ayar()));
        var p = com.raspel.erp.entity.ik.Personel.builder()
                .id(5L).sirketId(1L).ad("A").soyad("B").maas(new BigDecimal("43000")).build();
        when(personelRepository.findBySirketIdAndAktifTrue(1L)).thenReturn(List.of(p));
        when(maasBordroRepository.existsBySirketIdAndYilAndAyAndPersonelId(1L, 2026, 8, 5L)).thenReturn(false);
        when(maasBordroRepository.kumulatifMatrah(1L, 5L, 2026, 8)).thenReturn(new BigDecimal("150000"));
        when(maasBordroRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var sonuc = servis.topluUret(1L, 2026, 8);

        assertEquals(1, sonuc.get("uretilen"));
        verify(maasBordroRepository).kumulatifMatrah(1L, 5L, 2026, 8);
    }

    @Test
    void topluOnizleme_durumlariBelirler() {
        when(bordroAyarRepository.findBySirketIdAndYil(1L, 2026)).thenReturn(Optional.of(ayar()));
        var p1 = com.raspel.erp.entity.ik.Personel.builder().id(5L).sirketId(1L).ad("A").soyad("B").maas(new BigDecimal("43000")).build();
        var p2 = com.raspel.erp.entity.ik.Personel.builder().id(6L).sirketId(1L).ad("C").soyad("D").maas(BigDecimal.ZERO).build();
        var p3 = com.raspel.erp.entity.ik.Personel.builder().id(7L).sirketId(1L).ad("E").soyad("F").maas(new BigDecimal("50000")).build();
        when(personelRepository.findBySirketIdAndAktifTrue(1L)).thenReturn(List.of(p1, p2, p3));
        when(maasBordroRepository.existsBySirketIdAndYilAndAyAndPersonelId(eq(1L), eq(2026), eq(8), anyLong()))
                .thenAnswer(inv -> Long.valueOf(7L).equals(inv.getArgument(3)));
        when(maasBordroRepository.kumulatifMatrah(eq(1L), anyLong(), eq(2026), eq(8))).thenReturn(BigDecimal.ZERO);

        var sonuc = servis.topluOnizleme(1L, 2026, 8);

        assertEquals(3, sonuc.size());
        assertEquals("UYGUN", sonuc.get(0).getDurum());
        assertEquals("MAAS_YOK", sonuc.get(1).getDurum());
        assertEquals("ZATEN_VAR", sonuc.get(2).getDurum());
        assertNotNull(sonuc.get(0).getNetMaas());
    }
}
