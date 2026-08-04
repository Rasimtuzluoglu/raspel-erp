package com.raspel.erp.service.finans;

import com.raspel.erp.entity.ticaret.Fatura;
import com.raspel.erp.entity.finans.BankaHareketi;
import com.raspel.erp.repository.ticaret.FaturaRepository;
import com.raspel.erp.repository.finans.BankaHareketiRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import com.raspel.erp.entity.finans.Hareket;

@ExtendWith(MockitoExtension.class)
class BankaMutabakatServiceTest {

    @Mock private BankaHareketiRepository bankaHareketiRepository;
    @Mock private FaturaRepository faturaRepository;

    @InjectMocks private BankaMutabakatService bankaMutabakatService;

    private Fatura fatura(Long id, BigDecimal tutar, LocalDate tarih, String durum) {
        return Fatura.builder()
                .id(id).faturaNumarasi("FTR-" + id)
                .tur(Fatura.FaturaTur.SATIS).durum(Fatura.FaturaDurum.KESILDI)
                .tarih(tarih).genelToplam(tutar).kalanTutar(tutar)
                .odemeDurumu(durum).sirketId(1L)
                .build();
    }

    @Test
    void otomatikEslestir_tutarVeTarihEslesirseEslestirir() {
        LocalDate bugun = LocalDate.now();
        BankaHareketi hareket = BankaHareketi.builder()
                .id(1L).bankaId(10L).tarih(bugun)
                .borc(BigDecimal.valueOf(1000)).alacak(BigDecimal.ZERO)
                .eslestirildi(false).sirketId(1L).build();
        Fatura eslesenFatura = fatura(5L, BigDecimal.valueOf(1000), bugun.minusDays(1), "ODENMEDI");

        when(bankaHareketiRepository.findByBankaIdAndEslestirildiFalse(10L)).thenReturn(List.of(hareket));
        when(faturaRepository.findAll()).thenReturn(List.of(eslesenFatura));
        when(bankaHareketiRepository.save(any(BankaHareketi.class))).thenAnswer(inv -> inv.getArgument(0));
        when(bankaHareketiRepository.findByBankaIdOrderByTarihDesc(10L)).thenReturn(List.of(hareket));

        bankaMutabakatService.otomatikEslestir(10L, 1L);

        assertTrue(hareket.getEslestirildi());
        assertEquals(5L, hareket.getEslesenFaturaId());
    }

    @Test
    void otomatikEslestir_tutarEslesmezseEslestirmez() {
        LocalDate bugun = LocalDate.now();
        BankaHareketi hareket = BankaHareketi.builder()
                .id(1L).bankaId(10L).tarih(bugun)
                .borc(BigDecimal.valueOf(750)).alacak(BigDecimal.ZERO)
                .eslestirildi(false).sirketId(1L).build();
        Fatura farkliFatura = fatura(5L, BigDecimal.valueOf(1000), bugun, "ODENMEDI");

        when(bankaHareketiRepository.findByBankaIdAndEslestirildiFalse(10L)).thenReturn(List.of(hareket));
        when(faturaRepository.findAll()).thenReturn(List.of(farkliFatura));
        when(bankaHareketiRepository.findByBankaIdOrderByTarihDesc(10L)).thenReturn(List.of(hareket));

        bankaMutabakatService.otomatikEslestir(10L, 1L);

        assertFalse(hareket.getEslestirildi());
        assertNull(hareket.getEslesenFaturaId());
    }

    @Test
    void eslestir_manuelEslesmeKurar() {
        BankaHareketi hareket = BankaHareketi.builder().id(1L).bankaId(10L).eslestirildi(false).build();
        when(bankaHareketiRepository.findById(1L)).thenReturn(Optional.of(hareket));
        when(faturaRepository.existsById(5L)).thenReturn(true);
        when(bankaHareketiRepository.save(any(BankaHareketi.class))).thenAnswer(inv -> inv.getArgument(0));

        var dto = bankaMutabakatService.eslestir(1L, 5L);

        assertTrue(dto.getEslestirildi());
        assertEquals(5L, dto.getEslesenFaturaId());
    }

    @Test
    void eslestirmeyiKaldir_eslesmeyiSifirlar() {
        BankaHareketi hareket = BankaHareketi.builder().id(1L).bankaId(10L).eslestirildi(true).eslesenFaturaId(5L).build();
        when(bankaHareketiRepository.findById(1L)).thenReturn(Optional.of(hareket));
        when(bankaHareketiRepository.save(any(BankaHareketi.class))).thenAnswer(inv -> inv.getArgument(0));

        var dto = bankaMutabakatService.eslestirmeyiKaldir(1L);

        assertFalse(dto.getEslestirildi());
        assertNull(dto.getEslesenFaturaId());
    }
}