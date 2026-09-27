package com.raspel.erp.service.ik;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.raspel.erp.config.TenantChecker;
import com.raspel.erp.dto.ik.BordroHesaplamaDTO;
import com.raspel.erp.entity.ik.BordroAyar;
import com.raspel.erp.repository.ik.BordroAyarRepository;
import com.raspel.erp.repository.ik.MaasBordroRepository;
import com.raspel.erp.repository.ik.PersonelRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
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
}
