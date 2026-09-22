package com.raspel.erp.service.sube;

import com.raspel.erp.config.TenantChecker;
import com.raspel.erp.dto.sube.DepoDTO;
import com.raspel.erp.dto.sube.DepoStokDTO;
import com.raspel.erp.entity.sube.Depo;
import com.raspel.erp.entity.sube.DepoStok;
import com.raspel.erp.exception.BusinessException;
import com.raspel.erp.exception.ResourceNotFoundException;
import com.raspel.erp.repository.envanter.StokRepository;
import com.raspel.erp.repository.sube.DepoRepository;
import com.raspel.erp.repository.sube.DepoStokRepository;
import com.raspel.erp.repository.sube.SubeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DepoServiceTest {

    @Mock private DepoRepository depoRepository;
    @Mock private DepoStokRepository depoStokRepository;
    @Mock private SubeRepository subeRepository;
    @Mock private StokRepository stokRepository;
    @Mock private TenantChecker tenantChecker;
    @Mock private com.raspel.erp.service.envanter.StokService stokService;
    @InjectMocks private DepoService depoService;

    private Depo ornekDepo(Long id) {
        return Depo.builder()
                .id(id).ad("Ana Depo").adres("İstanbul")
                .subeId(1L).sirketId(1L).aktif(true)
                .olusturmaTarihi(LocalDateTime.now())
                .build();
    }

    @Test
    void tumunuGetir_returnsPage() {
        when(subeRepository.findBySirketIdOrderByAdAsc(anyLong(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        when(depoRepository.findBySirketIdOrderByAdAsc(anyLong(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(ornekDepo(1L))));
        var sonuc = depoService.tumunuGetir(1L, Pageable.unpaged());
        assertEquals(1, sonuc.getContent().size());
        assertEquals("Ana Depo", sonuc.getContent().get(0).getAd());
    }

    @Test
    void getir_returnsById() {
        when(depoRepository.findById(1L)).thenReturn(Optional.of(ornekDepo(1L)));
        when(subeRepository.findById(1L)).thenReturn(Optional.empty());
        var sonuc = depoService.getir(1L);
        assertEquals("Ana Depo", sonuc.getAd());
    }

    @Test
    void getir_notFound_throws() {
        when(depoRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> depoService.getir(99L));
    }

    @Test
    void olustur_creates() {
        DepoDTO dto = DepoDTO.builder().ad("Yeni Depo").subeId(1L).sirketId(1L).build();
        when(depoRepository.save(any(Depo.class))).thenAnswer(inv -> {
            Depo d = inv.getArgument(0);
            d.setId(1L);
            return d;
        });
        var sonuc = depoService.olustur(dto);
        assertEquals("Yeni Depo", sonuc.getAd());
    }

    @Test
    void stokEkle_globalHareketOlusturur() {
        when(depoRepository.findById(1L)).thenReturn(Optional.of(ornekDepo(1L)));
        when(stokRepository.findBySirketIdOrderByAd(1L)).thenReturn(List.of());
        when(depoStokRepository.findByDepoId(1L)).thenReturn(List.of());

        depoService.stokEkle(1L, 2L, new BigDecimal("5"));

        verify(stokService).hareketEkle(argThat(d -> "GIRIS".equals(d.getTur())
                && Long.valueOf(1L).equals(d.getDepoId())
                && d.getMiktar().compareTo(new BigDecimal("5")) == 0));
    }

    @Test
    void stokCikar_yetersizStok_throws() {
        DepoStok mevcut = DepoStok.builder().depoId(1L).stokId(2L).miktar(new BigDecimal("3")).build();
        when(depoRepository.findById(1L)).thenReturn(Optional.of(ornekDepo(1L)));
        when(depoStokRepository.findByDepoIdAndStokIdForUpdate(1L, 2L)).thenReturn(Optional.of(mevcut));

        assertThrows(BusinessException.class, () -> depoService.stokCikar(1L, 2L, new BigDecimal("10")));
    }

    @Test
    void stokCikar_azaltirMiktari() {
        DepoStok mevcut = DepoStok.builder().depoId(1L).stokId(2L).miktar(new BigDecimal("20")).build();
        when(depoRepository.findById(1L)).thenReturn(Optional.of(ornekDepo(1L)));
        when(depoStokRepository.findByDepoIdAndStokIdForUpdate(1L, 2L)).thenReturn(Optional.of(mevcut));
        when(stokRepository.findBySirketIdOrderByAd(1L)).thenReturn(List.of());
        when(depoStokRepository.findByDepoId(1L)).thenReturn(List.of(mevcut));

        depoService.stokCikar(1L, 2L, new BigDecimal("8"));

        verify(stokService).hareketEkle(argThat(d -> "CIKIS".equals(d.getTur())
                && Long.valueOf(1L).equals(d.getDepoId())
                && d.getMiktar().compareTo(new BigDecimal("8")) == 0));
    }

    @Test
    void stokTransfer_ikisiniDeYapar() {
        DepoStok kaynak = DepoStok.builder().depoId(1L).stokId(2L).miktar(new BigDecimal("50")).build();
        when(depoRepository.findById(1L)).thenReturn(Optional.of(ornekDepo(1L)));
        when(depoRepository.findById(3L)).thenReturn(Optional.of(ornekDepo(3L)));
        when(depoStokRepository.findByDepoIdAndStokIdForUpdate(1L, 2L)).thenReturn(Optional.of(kaynak));
        when(stokRepository.findBySirketIdOrderByAd(anyLong())).thenReturn(List.of());
        when(depoStokRepository.findByDepoId(anyLong())).thenReturn(List.of());

        depoService.stokTransfer(1L, 3L, 2L, new BigDecimal("10"));

        verify(stokService).hareketEkle(argThat(d -> "CIKIS".equals(d.getTur()) && Long.valueOf(1L).equals(d.getDepoId())));
        verify(stokService).hareketEkle(argThat(d -> "GIRIS".equals(d.getTur()) && Long.valueOf(3L).equals(d.getDepoId())));
    }

    @Test
    void stokDagilimi_depoKiriliminiDoner() {
        when(depoRepository.findBySirketIdOrderByAdAsc(eq(1L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(ornekDepo(1L))));
        DepoStok ds = DepoStok.builder().id(5L).depoId(1L).stokId(2L).miktar(new BigDecimal("7")).build();
        when(depoStokRepository.findByDepoIdIn(any())).thenReturn(List.of(ds));

        var sonuc = depoService.stokDagilimi(1L);

        assertEquals(1, sonuc.size());
        assertEquals("Ana Depo", sonuc.get(0).getDepoAdi());
        assertEquals(0, sonuc.get(0).getMiktar().compareTo(new BigDecimal("7")));
    }

    @Test
    void stokDagilimi_depoYoksaBosDoner() {
        when(depoRepository.findBySirketIdOrderByAdAsc(eq(1L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        assertTrue(depoService.stokDagilimi(1L).isEmpty());
    }

    @Test
    void sil_deletes() {
        when(depoRepository.findById(1L)).thenReturn(Optional.of(ornekDepo(1L)));
        depoService.sil(1L);
        verify(depoRepository).deleteById(1L);
    }
}
