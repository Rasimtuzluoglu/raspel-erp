package com.raspel.erp.service.sistem;

import com.raspel.erp.dto.sistem.ChurnRiskDTO;
import com.raspel.erp.entity.finans.CariHesap;
import com.raspel.erp.entity.finans.Hareket;
import com.raspel.erp.entity.ticaret.Fatura;
import com.raspel.erp.repository.finans.CariHesapRepository;
import com.raspel.erp.repository.finans.HareketRepository;
import com.raspel.erp.repository.ticaret.FaturaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChurnAnalizServiceTest {

    @Mock
    private CariHesapRepository cariHesapRepository;
    @Mock
    private HareketRepository hareketRepository;
    @Mock
    private FaturaRepository faturaRepository;

    @InjectMocks
    private ChurnAnalizService churnAnalizService;

    private CariHesap cari(Long id, String ad) {
        return CariHesap.builder().id(id).ad(ad).build();
    }

    @Test
    void churnRiskiAnaliz_uzunSureIslemYapmayanYuksekRisk() {
        when(cariHesapRepository.findBySirketIdOrderByAdAsc(1L)).thenReturn(List.of(cari(1L, "A Müşteri")));

        CariHesap c = cari(1L, "A Müşteri");
        Hareket h = Hareket.builder().id(1L).cariHesap(c).tutar(new BigDecimal("100"))
                .hareketTarihi(LocalDate.now().minusDays(120)).tur(Hareket.HareketTuru.TAHSILAT).build();
        when(hareketRepository.findBySirketIdOrderByHareketTarihiDesc(eq(1L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(h)));
        when(faturaRepository.findBySirketIdOrderByTarihDesc(eq(1L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        List<ChurnRiskDTO> sonuc = churnAnalizService.churnRiskiAnaliz(1L);

        assertEquals(1, sonuc.size());
        assertEquals("YUKSEK", sonuc.get(0).getSeviye());
        assertTrue(sonuc.get(0).getSkor() >= 70);
    }

    @Test
    void churnRiskiAnaliz_aktifMusteriDusukRisk() {
        when(cariHesapRepository.findBySirketIdOrderByAdAsc(1L)).thenReturn(List.of(cari(1L, "Aktif Müşteri")));

        CariHesap c = cari(1L, "Aktif Müşteri");
        Fatura f = Fatura.builder().id(1L).cariHesap(c).genelToplam(new BigDecimal("500"))
                .tarih(LocalDate.now().minusDays(3)).build();
        when(hareketRepository.findBySirketIdOrderByHareketTarihiDesc(eq(1L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        when(faturaRepository.findBySirketIdOrderByTarihDesc(eq(1L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(f)));

        List<ChurnRiskDTO> sonuc = churnAnalizService.churnRiskiAnaliz(1L);

        assertEquals(1, sonuc.size());
        assertEquals("DUSUK", sonuc.get(0).getSeviye());
    }

    @Test
    void churnRiskiAnaliz_islemYapmayanMusteriAtlanir() {
        when(cariHesapRepository.findBySirketIdOrderByAdAsc(1L)).thenReturn(List.of(cari(1L, "Yeni Müşteri")));
        when(hareketRepository.findBySirketIdOrderByHareketTarihiDesc(eq(1L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        when(faturaRepository.findBySirketIdOrderByTarihDesc(eq(1L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        List<ChurnRiskDTO> sonuc = churnAnalizService.churnRiskiAnaliz(1L);

        assertTrue(sonuc.isEmpty());
    }
}
