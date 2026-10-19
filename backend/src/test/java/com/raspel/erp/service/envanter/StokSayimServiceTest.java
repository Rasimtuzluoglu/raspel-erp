package com.raspel.erp.service.envanter;

import com.raspel.erp.config.CacheYardimci;
import com.raspel.erp.config.TenantChecker;
import com.raspel.erp.dto.envanter.StokSayimDTO;
import com.raspel.erp.entity.envanter.Stok;
import com.raspel.erp.entity.envanter.StokSayim;
import com.raspel.erp.exception.BusinessException;
import com.raspel.erp.repository.envanter.StokRepository;
import com.raspel.erp.repository.envanter.StokSayimRepository;
import com.raspel.erp.service.sistem.BildirimService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StokSayimServiceTest {

    @Mock private StokSayimRepository stokSayimRepository;
    @Mock private StokRepository stokRepository;
    @Mock private com.raspel.erp.repository.envanter.StokHareketRepository stokHareketRepository;
    @Mock private TenantChecker tenantChecker;
    @Mock private CacheYardimci cacheYardimci;
    @Mock private BildirimService bildirimService;
    @Mock private com.raspel.erp.service.sube.DepoStokService depoStokService;
    @Mock private com.raspel.erp.service.envanter.MaliyetService maliyetService;
    @InjectMocks private StokSayimService stokSayimService;

    @Test
    void filtreliGetir_sayfaliDonerVeDTOyaMapEder() {
        Stok stok = Stok.builder().id(10L).ad("Masa").build();
        StokSayim sayim = StokSayim.builder()
                .id(1L).stok(stok).sirketId(1L)
                .beklenenMiktar(BigDecimal.TEN).sayilanMiktar(BigDecimal.ONE)
                .fark(BigDecimal.valueOf(-9)).durum("TASLAK")
                .build();
        Pageable pageable = PageRequest.of(0, 25);
        when(stokSayimRepository.filtreliGetir(eq(1L), any(), eq("TASLAK"), any(), any(), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(sayim)));

        var sonuc = stokSayimService.filtreliGetir(1L, "ma", "TASLAK", null, null, pageable);

        assertEquals(1, sonuc.getTotalElements());
        assertEquals("Masa", sonuc.getContent().get(0).getStokAdi());
        assertEquals("TASLAK", sonuc.getContent().get(0).getDurum());
    }

    @Test
    void ozet_sayimlariToplar() {
        when(stokSayimRepository.countBySirketId(1L)).thenReturn(5L);
        when(stokSayimRepository.countBySirketIdAndDurum(1L, "TASLAK")).thenReturn(2L);
        when(stokSayimRepository.countBySirketIdAndDurum(1L, "TAMAMLANDI")).thenReturn(2L);
        when(stokSayimRepository.countBySirketIdAndDurum(1L, "IPTAL")).thenReturn(1L);
        when(stokSayimRepository.countFarkliTamamlanan(1L)).thenReturn(1L);
        when(stokSayimRepository.countBySirketIdAndTarih(eq(1L), any())).thenReturn(3L);

        var sonuc = stokSayimService.ozet(1L);

        assertEquals(5L, sonuc.getToplam());
        assertEquals(2L, sonuc.getTaslak());
        assertEquals(2L, sonuc.getTamamlanan());
        assertEquals(1L, sonuc.getFarkli());
        assertEquals(3L, sonuc.getBugun());
    }

    @Test
    void olustur_farkiSunucudaHesaplarVeTaslakBaslatir() {
        Stok stok = Stok.builder().id(10L).ad("Masa").miktar(BigDecimal.TEN).build();
        when(stokRepository.findById(10L)).thenReturn(Optional.of(stok));
        when(stokSayimRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        StokSayimDTO dto = StokSayimDTO.builder()
                .tarih(LocalDate.of(2026, 10, 1)).stokId(10L)
                .beklenenMiktar(BigDecimal.TEN).sayilanMiktar(BigDecimal.valueOf(7))
                .fark(BigDecimal.valueOf(999))
                .durum("TAMAMLANDI")
                .build();

        StokSayimDTO sonuc = stokSayimService.olustur(dto, 1L);

        assertEquals(0, sonuc.getFark().compareTo(BigDecimal.valueOf(-3)));
        assertEquals("TASLAK", sonuc.getDurum());
    }

    @Test
    void olustur_negatifSayilanReddedilir() {
        Stok stok = Stok.builder().id(10L).ad("Masa").miktar(BigDecimal.TEN).build();
        when(stokRepository.findById(10L)).thenReturn(Optional.of(stok));
        StokSayimDTO dto = StokSayimDTO.builder()
                .tarih(LocalDate.now()).stokId(10L)
                .sayilanMiktar(BigDecimal.valueOf(-1))
                .build();

        assertThrows(BusinessException.class, () -> stokSayimService.olustur(dto, 1L));
        verify(stokSayimRepository, never()).save(any());
    }

    @Test
    void guncelle_tamamlanmisSayimReddedilir() {
        StokSayim sayim = StokSayim.builder().id(1L).sirketId(1L).durum("TAMAMLANDI").build();
        when(stokSayimRepository.findById(1L)).thenReturn(Optional.of(sayim));

        assertThrows(BusinessException.class, () ->
                stokSayimService.guncelle(1L, StokSayimDTO.builder().sayilanMiktar(BigDecimal.ONE).build()));
        verify(stokSayimRepository, never()).save(any());
    }

    @Test
    void tara_ayniGunTaslakSayimaEkler() {
        Stok stok = Stok.builder().id(10L).ad("Masa").miktar(BigDecimal.TEN).build();
        when(stokRepository.findBySirketIdAndBarkod(1L, "123")).thenReturn(List.of(stok));
        StokSayim sayim = StokSayim.builder()
                .id(1L).stok(stok).sirketId(1L)
                .beklenenMiktar(BigDecimal.TEN).sayilanMiktar(BigDecimal.ONE).durum("TASLAK")
                .build();
        when(stokSayimRepository.findFirstBySirketIdAndStokIdAndDurumAndTarihOrderByOlusturmaTarihiDesc(
                eq(1L), eq(10L), eq("TASLAK"), any())).thenReturn(Optional.of(sayim));
        when(stokSayimRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var sonuc = stokSayimService.tara("123", BigDecimal.ONE, 1L);

        assertEquals(0, sonuc.getSayilanMiktar().compareTo(BigDecimal.valueOf(2)));
    }
}
