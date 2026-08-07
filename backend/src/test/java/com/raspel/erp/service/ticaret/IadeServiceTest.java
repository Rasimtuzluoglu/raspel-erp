package com.raspel.erp.service.ticaret;

import com.raspel.erp.config.TenantChecker;
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
        when(stokRepository.findById(1L)).thenReturn(Optional.of(stok));
        when(stokRepository.save(any(Stok.class))).thenAnswer(inv -> inv.getArgument(0));

        iadeService.olustur(dto, 1L);

        assertEquals(0, stok.getMiktar().compareTo(new BigDecimal("15")));
        verify(stokHareketRepository, times(1)).save(any());
    }

    @Test
    void sil_deletes() {
        when(iadeRepository.findById(1L)).thenReturn(Optional.of(ornekIade(1L)));
        iadeService.sil(1L);
        verify(iadeKalemRepository).deleteByIadeId(1L);
        verify(iadeRepository).deleteById(1L);
    }
}
