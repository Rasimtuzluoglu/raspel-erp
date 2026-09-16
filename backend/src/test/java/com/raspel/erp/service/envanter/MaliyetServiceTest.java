package com.raspel.erp.service.envanter;

import com.raspel.erp.entity.envanter.Stok;
import com.raspel.erp.entity.envanter.StokMaliyetHareket;
import com.raspel.erp.repository.envanter.StokMaliyetHareketRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MaliyetServiceTest {

    @Mock private StokMaliyetHareketRepository stokMaliyetHareketRepository;
    @InjectMocks private MaliyetService maliyetService;

    private Stok stok(String miktar, String ortalama, String tedarikci, String fiyat) {
        return Stok.builder().id(1L).ad("Ürün").sirketId(1L)
                .miktar(miktar != null ? new BigDecimal(miktar) : null)
                .ortalamaMaliyet(ortalama != null ? new BigDecimal(ortalama) : null)
                .tedarikciFiyat(tedarikci != null ? new BigDecimal(tedarikci) : null)
                .fiyat(fiyat != null ? new BigDecimal(fiyat) : null)
                .build();
    }

    @Test
    void ortalamaMaliyet_ortalamaOnce() {
        assertEquals(0, maliyetService.ortalamaMaliyet(stok("10", "12.5", "11", "10"))
                .compareTo(new BigDecimal("12.5")));
    }

    @Test
    void ortalamaMaliyet_tedarikciFiyatFallback() {
        assertEquals(0, maliyetService.ortalamaMaliyet(stok("10", null, "11", "10"))
                .compareTo(new BigDecimal("11")));
    }

    @Test
    void ortalamaMaliyet_listeFiyatFallback() {
        assertEquals(0, maliyetService.ortalamaMaliyet(stok("10", null, null, "9.75"))
                .compareTo(new BigDecimal("9.75")));
    }

    @Test
    void ortalamaMaliyet_nullStokSifir() {
        assertEquals(0, maliyetService.ortalamaMaliyet(null).compareTo(BigDecimal.ZERO));
    }

    @Test
    void girisIsle_ilkGirisOrtalamayiFiyataEsitler() {
        Stok s = stok("0", null, null, "5");
        BigDecimal sonuc = maliyetService.girisIsle(s, BigDecimal.ZERO, new BigDecimal("10"),
                new BigDecimal("8.5"), 1L, "FATURA", 9L);
        assertEquals(0, sonuc.compareTo(new BigDecimal("8.5000")));
        assertEquals(0, s.getOrtalamaMaliyet().compareTo(new BigDecimal("8.5000")));
    }

    @Test
    void girisIsle_agirlikliOrtalamaHesaplar() {
        Stok s = stok("10", "10", null, null);
        BigDecimal sonuc = maliyetService.girisIsle(s, new BigDecimal("10"), new BigDecimal("10"),
                new BigDecimal("20"), 1L, "FATURA", 9L);
        assertEquals(0, sonuc.compareTo(new BigDecimal("15.0000")));
    }

    @Test
    void girisIsle_sifirFiyatMevcutOrtalamayiKorur() {
        Stok s = stok("10", "10", null, null);
        BigDecimal sonuc = maliyetService.girisIsle(s, new BigDecimal("10"), new BigDecimal("10"),
                BigDecimal.ZERO, 1L, "MANUEL", null);
        assertEquals(0, sonuc.compareTo(new BigDecimal("10")));
    }

    @Test
    void girisIsle_negatifMiktarOrtalamayiDegistirmez() {
        Stok s = stok("10", "10", null, null);
        BigDecimal sonuc = maliyetService.girisIsle(s, new BigDecimal("10"), BigDecimal.ZERO,
                new BigDecimal("99"), 1L, "MANUEL", null);
        assertEquals(0, sonuc.compareTo(new BigDecimal("10")));
        verify(stokMaliyetHareketRepository, never()).save(any());
    }

    @Test
    void cikisIsle_guncelOrtalamayiDonerOrtalamayiDegistirmez() {
        Stok s = stok("10", "12", null, null);
        BigDecimal sonuc = maliyetService.cikisIsle(s, new BigDecimal("4"), new BigDecimal("6"),
                1L, "FATURA", 9L);
        assertEquals(0, sonuc.compareTo(new BigDecimal("12")));
        assertEquals(0, s.getOrtalamaMaliyet().compareTo(new BigDecimal("12")));
    }

    @Test
    void cikisIsle_deftereYazar() {
        Stok s = stok("10", "12", null, null);
        maliyetService.cikisIsle(s, new BigDecimal("4"), new BigDecimal("6"), 1L, "FATURA", 9L);

        ArgumentCaptor<StokMaliyetHareket> captor = ArgumentCaptor.forClass(StokMaliyetHareket.class);
        verify(stokMaliyetHareketRepository).save(captor.capture());
        StokMaliyetHareket h = captor.getValue();
        assertEquals("CIKIS", h.getTur());
        assertEquals(0, h.getBirimMaliyet().compareTo(new BigDecimal("12.0000")));
        assertEquals(0, h.getToplamMaliyet().compareTo(new BigDecimal("48.00")));
        assertEquals(0, h.getKalanMiktar().compareTo(new BigDecimal("6.0000")));
    }

    @Test
    void cikisIsle_sifirMiktarDeftereYazmaz() {
        Stok s = stok("10", "12", null, null);
        BigDecimal sonuc = maliyetService.cikisIsle(s, BigDecimal.ZERO, new BigDecimal("10"),
                1L, "FATURA", 9L);
        assertEquals(0, sonuc.compareTo(new BigDecimal("12")));
        verify(stokMaliyetHareketRepository, never()).save(any());
    }

    @Test
    void defterHatasiIsAkisiniBloklamaz() {
        Stok s = stok("10", "10", null, null);
        when(stokMaliyetHareketRepository.save(any(StokMaliyetHareket.class)))
                .thenThrow(new RuntimeException("db down"));
        assertDoesNotThrow(() -> maliyetService.girisIsle(s, new BigDecimal("10"), new BigDecimal("5"),
                new BigDecimal("20"), 1L, "FATURA", 1L));
        assertEquals(0, s.getOrtalamaMaliyet().compareTo(new BigDecimal("13.3333")));
    }
}
