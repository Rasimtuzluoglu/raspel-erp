package com.raspel.erp.service.sistem;

import com.raspel.erp.dto.sistem.VeriAktarimDTO;
import com.raspel.erp.dto.sistem.VeriAktarimSonucDTO;
import com.raspel.erp.entity.envanter.Stok;
import com.raspel.erp.entity.finans.CariHesap;
import com.raspel.erp.entity.sistem.Sirket;
import com.raspel.erp.exception.BusinessException;
import com.raspel.erp.repository.envanter.StokRepository;
import com.raspel.erp.repository.finans.CariHesapRepository;
import com.raspel.erp.repository.sistem.SirketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VeriAktarimServiceTest {

    @Mock
    private StokRepository stokRepository;

    @Mock
    private CariHesapRepository cariHesapRepository;

    @Mock
    private SirketRepository sirketRepository;

    @InjectMocks
    private VeriAktarimService veriAktarimService;

    private Sirket kaynakSirket;
    private Sirket hedefSirket;

    @BeforeEach
    void setUp() {
        kaynakSirket = Sirket.builder().id(1L).ad("Kaynak Sirket").build();
        hedefSirket = Sirket.builder().id(2L).ad("Hedef Sirket").build();
    }

    @Test
    void aktarimYap_stoklariAktar_kopyalar() {
        when(sirketRepository.findById(1L)).thenReturn(Optional.of(kaynakSirket));
        when(sirketRepository.findById(2L)).thenReturn(Optional.of(hedefSirket));

        Stok stok = Stok.builder().id(100L).ad("Stok 1").stokKodu("S01").miktar(BigDecimal.TEN).build();
        when(stokRepository.findBySirketIdOrderByAd(eq(1L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(stok)));
        when(stokRepository.findBySirketIdAndStokKodu(2L, "S01")).thenReturn(Optional.empty());

        VeriAktarimDTO dto = VeriAktarimDTO.builder()
                .kaynakSirketId(1L)
                .hedefSirketId(2L)
                .stoklariAktar(true)
                .build();

        VeriAktarimSonucDTO sonuc = veriAktarimService.aktarimYap(dto);

        assertEquals(1, sonuc.getAktarilanStokSayisi());
        assertEquals(0, sonuc.getAtlananStokSayisi());

        ArgumentCaptor<Stok> stokCaptor = ArgumentCaptor.forClass(Stok.class);
        verify(stokRepository).save(stokCaptor.capture());
        Stok savedStok = stokCaptor.getValue();
        assertEquals("Stok 1", savedStok.getAd());
        assertEquals("S01", savedStok.getStokKodu());
        assertEquals(BigDecimal.ZERO, savedStok.getMiktar());
        assertEquals(2L, savedStok.getSirketId());
    }

    @Test
    void aktarimYap_carileriAktar_kopyalar() {
        when(sirketRepository.findById(1L)).thenReturn(Optional.of(kaynakSirket));
        when(sirketRepository.findById(2L)).thenReturn(Optional.of(hedefSirket));

        CariHesap cari = CariHesap.builder().id(200L).ad("Cari 1").vergiNumarasi("12345").bakiye(BigDecimal.TEN).build();
        when(cariHesapRepository.findBySirketId(eq(1L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(cari)));
        when(cariHesapRepository.findBySirketId(eq(2L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        VeriAktarimDTO dto = VeriAktarimDTO.builder()
                .kaynakSirketId(1L)
                .hedefSirketId(2L)
                .carileriAktar(true)
                .bakiyeleriSifirla(true)
                .build();

        VeriAktarimSonucDTO sonuc = veriAktarimService.aktarimYap(dto);

        assertEquals(1, sonuc.getAktarilanCariSayisi());
        assertEquals(0, sonuc.getAtlananCariSayisi());

        ArgumentCaptor<CariHesap> cariCaptor = ArgumentCaptor.forClass(CariHesap.class);
        verify(cariHesapRepository).save(cariCaptor.capture());
        CariHesap savedCari = cariCaptor.getValue();
        assertEquals("Cari 1", savedCari.getAd());
        assertEquals("12345", savedCari.getVergiNumarasi());
        assertEquals(BigDecimal.ZERO, savedCari.getBakiye());
        assertEquals(2L, savedCari.getSirketId());
    }

    @Test
    void aktarimYap_mukerrerStokAtlar() {
        when(sirketRepository.findById(1L)).thenReturn(Optional.of(kaynakSirket));
        when(sirketRepository.findById(2L)).thenReturn(Optional.of(hedefSirket));

        Stok stok = Stok.builder().id(100L).ad("Stok 1").stokKodu("S01").build();
        when(stokRepository.findBySirketIdOrderByAd(eq(1L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(stok)));
        when(stokRepository.findBySirketIdAndStokKodu(2L, "S01")).thenReturn(Optional.of(stok));

        VeriAktarimDTO dto = VeriAktarimDTO.builder()
                .kaynakSirketId(1L)
                .hedefSirketId(2L)
                .stoklariAktar(true)
                .build();

        VeriAktarimSonucDTO sonuc = veriAktarimService.aktarimYap(dto);

        assertEquals(0, sonuc.getAktarilanStokSayisi());
        assertEquals(1, sonuc.getAtlananStokSayisi());
        verify(stokRepository, never()).save(any());
    }

    @Test
    void aktarimYap_ayniSirketHata() {
        when(sirketRepository.findById(1L)).thenReturn(Optional.of(kaynakSirket));
        when(sirketRepository.findById(1L)).thenReturn(Optional.of(kaynakSirket));

        VeriAktarimDTO dto = VeriAktarimDTO.builder()
                .kaynakSirketId(1L)
                .hedefSirketId(1L)
                .build();

        assertThrows(BusinessException.class, () -> veriAktarimService.aktarimYap(dto));
    }

    @Test
    void onizleme_dogru_sayilar_doner() {
        when(sirketRepository.findById(1L)).thenReturn(Optional.of(kaynakSirket));
        when(sirketRepository.findById(2L)).thenReturn(Optional.of(hedefSirket));
        when(stokRepository.countBySirketId(1L)).thenReturn(5L);
        when(cariHesapRepository.findBySirketId(eq(1L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(new CariHesap(), new CariHesap(), new CariHesap())));

        VeriAktarimSonucDTO sonuc = veriAktarimService.onizleme(1L, 2L);

        assertEquals(5, sonuc.getAktarilanStokSayisi());
        assertEquals(3, sonuc.getAktarilanCariSayisi());
        assertEquals("Kaynak Sirket", sonuc.getKaynakSirketAdi());
        assertEquals("Hedef Sirket", sonuc.getHedefSirketAdi());
    }
}
