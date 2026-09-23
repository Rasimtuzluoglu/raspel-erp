package com.raspel.erp.service.ticaret;

import com.raspel.erp.config.TenantChecker;
import com.raspel.erp.config.CacheYardimci;
import com.raspel.erp.dto.ticaret.IadeDTO;
import com.raspel.erp.dto.ticaret.IadeKalemDTO;
import com.raspel.erp.entity.envanter.Stok;
import com.raspel.erp.entity.ticaret.Iade;
import com.raspel.erp.entity.ticaret.IadeKalem;
import com.raspel.erp.exception.ResourceNotFoundException;
import com.raspel.erp.repository.envanter.StokHareketRepository;
import com.raspel.erp.repository.envanter.StokRepository;
import com.raspel.erp.repository.ticaret.IadeKalemRepository;
import com.raspel.erp.repository.ticaret.IadeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IadeServiceTest {

    @Mock private IadeRepository iadeRepository;
    @Mock private IadeKalemRepository iadeKalemRepository;
    @Mock private StokRepository stokRepository;
    @Mock private StokHareketRepository stokHareketRepository;
    @Mock private TenantChecker tenantChecker;
    @Mock private CacheYardimci cacheYardimci;
    @Mock private com.raspel.erp.repository.ticaret.FaturaRepository faturaRepository;
    @Mock private com.raspel.erp.repository.ticaret.FaturaKalemRepository faturaKalemRepository;
    @Mock private com.raspel.erp.service.finans.CariHesapService cariHesapService;
    @Mock private com.raspel.erp.service.sube.DepoStokService depoStokService;
    @Mock private com.raspel.erp.service.envanter.MaliyetService maliyetService;
    @Mock private com.raspel.erp.service.sistem.DonemService donemService;
    @InjectMocks private IadeService iadeService;

    private void hazirla() {
        ReflectionTestUtils.setField(iadeService, "varsayilanKdvOrani", new BigDecimal("20"));
    }

    private Iade ornekIade(Long id) {
        return Iade.builder()
                .id(id).faturaId(1L).tarih(LocalDate.now())
                .tutar(new BigDecimal("1200")).durum("TASLAK")
                .sirketId(1L).build();
    }

    @Test
    void tumunuGetir_returnsPage() {
        when(iadeRepository.findBySirketIdOrderByTarihDesc(anyLong(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(ornekIade(1L))));
        var sonuc = iadeService.tumunuGetir(1L, Pageable.unpaged());
        assertEquals(1, sonuc.getContent().size());
    }

    @Test
    void getir_returnsById() {
        hazirla();
        when(iadeRepository.findById(1L)).thenReturn(Optional.of(ornekIade(1L)));
        when(iadeKalemRepository.findByIadeId(1L)).thenReturn(List.of());
        var sonuc = iadeService.getir(1L);
        assertEquals("TASLAK", sonuc.getDurum());
    }

    @Test
    void getir_notFound_throws() {
        when(iadeRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> iadeService.getir(99L));
    }

    @Test
    void olustur_tutarHesapIcerir() {
        hazirla();
        IadeDTO dto = IadeDTO.builder()
                .tarih(LocalDate.now()).durum("TASLAK")
                .kalemler(List.of(IadeKalemDTO.builder()
                        .stokId(1L).aciklama("Ürün").miktar(new BigDecimal("2"))
                        .birimFiyat(new BigDecimal("100")).kdvOrani(new BigDecimal("20"))
                        .build()))
                .build();
        when(iadeRepository.save(any(Iade.class))).thenAnswer(inv -> {
            Iade i = inv.getArgument(0);
            i.setId(1L);
            return i;
        });
        when(iadeKalemRepository.findByIadeId(1L)).thenReturn(List.of());

        var sonuc = iadeService.olustur(dto, 1L);

        // 2 x 100 = 200 + %20 KDV = 240
        assertEquals(0, sonuc.getTutar().compareTo(new BigDecimal("240")));
    }

    @Test
    void olustur_tamamlandiStokHareketiIsler() {
        hazirla();
        IadeDTO dto = IadeDTO.builder()
                .tarih(LocalDate.now()).durum("TAMAMLANDI")
                .kalemler(List.of(IadeKalemDTO.builder()
                        .stokId(1L).aciklama("Ürün").miktar(new BigDecimal("5"))
                        .birimFiyat(new BigDecimal("10")).kdvOrani(new BigDecimal("20"))
                        .build()))
                .build();
        Stok stok = Stok.builder().id(1L).ad("Test Ürün").miktar(new BigDecimal("10")).build();
        when(iadeRepository.save(any(Iade.class))).thenAnswer(inv -> {
            Iade i = inv.getArgument(0);
            i.setId(1L);
            return i;
        });
        when(iadeKalemRepository.findByIadeId(1L)).thenReturn(List.of(
                IadeKalem.builder().iadeId(1L).stokId(1L)
                        .miktar(new BigDecimal("5")).build()
        ));
        when(stokRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(stok));
        when(stokRepository.save(any(Stok.class))).thenAnswer(inv -> inv.getArgument(0));

        iadeService.olustur(dto, 1L);

        assertEquals(0, stok.getMiktar().compareTo(new BigDecimal("15")));
        verify(stokHareketRepository, times(1)).save(any());
    }

    @Test
    void olustur_alisIadesi_stokDuser() {
        hazirla();
        IadeDTO dto = IadeDTO.builder()
                .tur("ALIS").tarih(LocalDate.now()).durum("TAMAMLANDI")
                .kalemler(List.of(IadeKalemDTO.builder()
                        .stokId(1L).aciklama("Ürün").miktar(new BigDecimal("5"))
                        .birimFiyat(new BigDecimal("10")).kdvOrani(new BigDecimal("20"))
                        .build()))
                .build();
        Stok stok = Stok.builder().id(1L).ad("Test Ürün").miktar(new BigDecimal("10")).build();
        when(iadeRepository.save(any(Iade.class))).thenAnswer(inv -> {
            Iade i = inv.getArgument(0);
            i.setId(1L);
            return i;
        });
        when(iadeKalemRepository.findByIadeId(1L)).thenReturn(List.of(
                IadeKalem.builder().iadeId(1L).stokId(1L)
                        .miktar(new BigDecimal("5")).build()
        ));
        when(stokRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(stok));
        when(stokRepository.save(any(Stok.class))).thenAnswer(inv -> inv.getArgument(0));

        iadeService.olustur(dto, 1L);

        assertEquals(0, stok.getMiktar().compareTo(new BigDecimal("5")));
        verify(stokHareketRepository, times(1)).save(any());
    }

    @Test
    void sil_deletes() {
        when(iadeRepository.findById(1L)).thenReturn(Optional.of(ornekIade(1L)));
        iadeService.sil(1L);
        verify(iadeKalemRepository).deleteByIadeId(1L);
        verify(iadeRepository).deleteById(1L);
    }

    @Test
    void sil_tamamlandi_throws() {
        Iade i = ornekIade(1L);
        i.setDurum("TAMAMLANDI");
        when(iadeRepository.findById(1L)).thenReturn(Optional.of(i));
        assertThrows(RuntimeException.class, () -> iadeService.sil(1L));
    }

    @Test
    void guncelle_tamamlandiIadeninKalemleriDegistirilemez() {
        Iade i = ornekIade(1L);
        i.setDurum("TAMAMLANDI");
        when(iadeRepository.findById(1L)).thenReturn(Optional.of(i));
        IadeDTO dto = IadeDTO.builder()
                .kalemler(List.of(IadeKalemDTO.builder()
                        .stokId(1L).miktar(new BigDecimal("99"))
                        .birimFiyat(new BigDecimal("1")).build()))
                .build();
        assertThrows(com.raspel.erp.exception.BusinessException.class, () -> iadeService.guncelle(1L, dto));
        verify(iadeKalemRepository, never()).deleteByIadeId(anyLong());
        verify(iadeRepository, never()).save(any());
    }

    @Test
    void guncelle_tamamlandiIadeTaslagaAlinamaz() {
        Iade i = ornekIade(1L);
        i.setDurum("TAMAMLANDI");
        when(iadeRepository.findById(1L)).thenReturn(Optional.of(i));
        IadeDTO dto = IadeDTO.builder().durum("TASLAK").build();
        assertThrows(com.raspel.erp.exception.BusinessException.class, () -> iadeService.guncelle(1L, dto));
    }

    @Test
    void guncelle_tamamlandiIadeninTutariDegistirilemez() {
        Iade i = ornekIade(1L);
        i.setDurum("TAMAMLANDI");
        when(iadeRepository.findById(1L)).thenReturn(Optional.of(i));
        IadeDTO dto = IadeDTO.builder().tutar(new BigDecimal("5000")).build();
        assertThrows(com.raspel.erp.exception.BusinessException.class, () -> iadeService.guncelle(1L, dto));
    }

    @Test
    void guncelle_taslakIadeGuncellenebilir() {
        hazirla();
        Iade i = ornekIade(1L);
        when(iadeRepository.findById(1L)).thenReturn(Optional.of(i));
        when(iadeKalemRepository.findByIadeId(1L)).thenReturn(List.of());
        when(iadeRepository.save(any(Iade.class))).thenAnswer(inv -> inv.getArgument(0));
        IadeDTO dto = IadeDTO.builder().durum("TAMAMLANDI").build();
        var sonuc = iadeService.guncelle(1L, dto);
        assertEquals("TAMAMLANDI", sonuc.getDurum());
    }

    @Test
    void iptal_alisIadesiYetersizStoktaHataVerir() {
        hazirla();
        Iade i = ornekIade(1L);
        i.setDurum("TAMAMLANDI");
        i.setTur("SATIS");
        when(iadeRepository.findById(1L)).thenReturn(Optional.of(i));
        when(iadeKalemRepository.findByIadeId(1L)).thenReturn(List.of(
                IadeKalem.builder().iadeId(1L).stokId(1L)
                        .miktar(new BigDecimal("5")).build()
        ));
        Stok stok = Stok.builder().id(1L).ad("Ürün").miktar(new BigDecimal("3")).build();
        when(stokRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(stok));
        IadeDTO dto = IadeDTO.builder().durum("IPTAL").build();
        assertThrows(com.raspel.erp.exception.BusinessException.class, () -> iadeService.guncelle(1L, dto));
        verify(stokRepository, never()).save(any());
    }

    @Test
    void durumGuncelle_tamamlandiCariBakiyeEtkisi() {
        hazirla();
        Iade iade = Iade.builder().id(1L).faturaId(1L).tur("SATIS")
                .tutar(new BigDecimal("240")).durum("TASLAK").sirketId(1L).build();
        com.raspel.erp.entity.finans.CariHesap cari = new com.raspel.erp.entity.finans.CariHesap();
        cari.setId(5L);
        com.raspel.erp.entity.ticaret.Fatura fatura = new com.raspel.erp.entity.ticaret.Fatura();
        fatura.setCariHesap(cari);
        fatura.setGenelToplam(new BigDecimal("240"));
        fatura.setKalanTutar(new BigDecimal("240"));
        fatura.setOdenenTutar(BigDecimal.ZERO);

        when(iadeRepository.findById(1L)).thenReturn(Optional.of(iade));
        when(iadeKalemRepository.findByIadeId(1L)).thenReturn(List.of());
        when(iadeRepository.save(any(Iade.class))).thenAnswer(inv -> inv.getArgument(0));
        when(faturaRepository.findById(1L)).thenReturn(Optional.of(fatura));

        iadeService.durumGuncelle(1L, "TAMAMLANDI");

        // Satis iadesi musteri borcunu 240 azaltir -> +240
        verify(cariHesapService).bakiyeGuncelle(5L, new BigDecimal("240"));
        // Iade faturaya yansimali: kalan 240 -> 0, durum ODENDI.
        assertEquals(0, fatura.getKalanTutar().compareTo(BigDecimal.ZERO));
        assertEquals("ODENDI", fatura.getOdemeDurumu());
        verify(faturaRepository).save(fatura);
    }

    @Test
    void olustur_iadeMiktariFaturayiAsarsa_reddedilir() {
        hazirla();
        com.raspel.erp.entity.ticaret.Fatura fatura = new com.raspel.erp.entity.ticaret.Fatura();
        fatura.setId(1L);
        fatura.setSirketId(1L);
        fatura.setGenelToplam(new BigDecimal("1000"));
        com.raspel.erp.entity.ticaret.FaturaKalem fk = com.raspel.erp.entity.ticaret.FaturaKalem.builder()
                .stokId(2L).adet(new BigDecimal("5")).build();
        when(faturaRepository.findById(1L)).thenReturn(Optional.of(fatura));
        when(faturaKalemRepository.findByFaturaId(1L)).thenReturn(List.of(fk));

        IadeKalemDTO kalem = IadeKalemDTO.builder().stokId(2L).miktar(new BigDecimal("10"))
                .birimFiyat(new BigDecimal("10")).kdvOrani(BigDecimal.ZERO).build();
        IadeDTO dto = IadeDTO.builder().faturaId(1L).tur("SATIS").tarih(java.time.LocalDate.now())
                .kalemler(List.of(kalem)).build();

        assertThrows(com.raspel.erp.exception.BusinessException.class,
                () -> iadeService.olustur(dto, 1L));
        verify(iadeRepository, never()).save(any());
    }

    @Test
    void olustur_kumulatifIadeMiktariAsarsa_reddedilir() {
        hazirla();
        com.raspel.erp.entity.ticaret.Fatura fatura = new com.raspel.erp.entity.ticaret.Fatura();
        fatura.setId(1L);
        fatura.setSirketId(1L);
        fatura.setGenelToplam(new BigDecimal("1000"));
        com.raspel.erp.entity.ticaret.FaturaKalem fk = com.raspel.erp.entity.ticaret.FaturaKalem.builder()
                .stokId(2L).adet(new BigDecimal("5")).build();
        when(faturaRepository.findById(1L)).thenReturn(Optional.of(fatura));
        when(faturaKalemRepository.findByFaturaId(1L)).thenReturn(List.of(fk));
        // Önceki tamamlanmış iade: 4 adet
        com.raspel.erp.entity.ticaret.Iade onceki = com.raspel.erp.entity.ticaret.Iade.builder()
                .id(7L).faturaId(1L).tur("SATIS").tutar(new BigDecimal("40")).durum("TAMAMLANDI").sirketId(1L).build();
        when(iadeRepository.findByFaturaIdInAndSirketId(any(), eq(1L))).thenReturn(List.of(onceki));
        com.raspel.erp.entity.ticaret.IadeKalem oncekiKalem = com.raspel.erp.entity.ticaret.IadeKalem.builder()
                .iadeId(7L).stokId(2L).miktar(new BigDecimal("4")).build();
        when(iadeKalemRepository.findByIadeId(7L)).thenReturn(List.of(oncekiKalem));

        // Bu iade 2 adet -> toplam 6 > 5
        IadeKalemDTO kalem = IadeKalemDTO.builder().stokId(2L).miktar(new BigDecimal("2"))
                .birimFiyat(new BigDecimal("10")).kdvOrani(BigDecimal.ZERO).build();
        IadeDTO dto = IadeDTO.builder().faturaId(1L).tur("SATIS").tarih(java.time.LocalDate.now())
                .kalemler(List.of(kalem)).build();

        assertThrows(com.raspel.erp.exception.BusinessException.class,
                () -> iadeService.olustur(dto, 1L));
        verify(iadeRepository, never()).save(any());
    }

    @Test
    void olustur_kilitliDonem_reddedilir() {
        IadeDTO dto = IadeDTO.builder().tur("SATIS").tarih(java.time.LocalDate.now())
                .tutar(new BigDecimal("100")).build();
        doThrow(new com.raspel.erp.exception.BusinessException("Bu tarih kilitli"))
                .when(donemService).kilitKontrol(any(), any(), anyString());

        assertThrows(com.raspel.erp.exception.BusinessException.class,
                () -> iadeService.olustur(dto, 1L));
        verify(iadeRepository, never()).save(any());
    }

    @Test
    void durumGuncelle_kilitliDonem_reddedilir() {
        Iade iade = Iade.builder().id(1L).faturaId(1L).tur("SATIS")
                .tutar(new BigDecimal("240")).durum("TASLAK").sirketId(1L).build();
        when(iadeRepository.findById(1L)).thenReturn(Optional.of(iade));
        doThrow(new com.raspel.erp.exception.BusinessException("Bu tarih kilitli"))
                .when(donemService).kilitKontrol(any(), any(), anyString());

        assertThrows(com.raspel.erp.exception.BusinessException.class,
                () -> iadeService.durumGuncelle(1L, "TAMAMLANDI"));
    }
}
