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
    void stokEkle_artirirMiktari() {
        DepoStok mevcut = DepoStok.builder().depoId(1L).stokId(2L).miktar(new BigDecimal("10")).build();
        when(depoRepository.findById(1L)).thenReturn(Optional.of(ornekDepo(1L)));
        when(depoStokRepository.findByDepoIdAndStokId(1L, 2L)).thenReturn(Optional.of(mevcut));
        when(stokRepository.findAll()).thenReturn(List.of());
        when(depoStokRepository.findByDepoId(1L)).thenReturn(List.of(mevcut));

        depoService.stokEkle(1L, 2L, new BigDecimal("5"));

        assertEquals(0, mevcut.getMiktar().compareTo(new BigDecimal("15")));
        verify(depoStokRepository).save(mevcut);
    }

    @Test
    void stokCikar_yetersizStok_throws() {
        DepoStok mevcut = DepoStok.builder().depoId(1L).stokId(2L).miktar(new BigDecimal("3")).build();
        when(depoRepository.findById(1L)).thenReturn(Optional.of(ornekDepo(1L)));
        when(depoStokRepository.findByDepoIdAndStokId(1L, 2L)).thenReturn(Optional.of(mevcut));

        assertThrows(BusinessException.class, () -> depoService.stokCikar(1L, 2L, new BigDecimal("10")));
    }

    @Test
    void stokCikar_azaltirMiktari() {
        DepoStok mevcut = DepoStok.builder().depoId(1L).stokId(2L).miktar(new BigDecimal("20")).build();
        when(depoRepository.findById(1L)).thenReturn(Optional.of(ornekDepo(1L)));
        when(depoStokRepository.findByDepoIdAndStokId(1L, 2L)).thenReturn(Optional.of(mevcut));
        when(stokRepository.findAll()).thenReturn(List.of());
        when(depoStokRepository.findByDepoId(1L)).thenReturn(List.of(mevcut));

        depoService.stokCikar(1L, 2L, new BigDecimal("8"));

        assertEquals(0, mevcut.getMiktar().compareTo(new BigDecimal("12")));
    }

    @Test
    void stokTransfer_ikisiniDeYapar() {
        DepoStok kaynak = DepoStok.builder().depoId(1L).stokId(2L).miktar(new BigDecimal("50")).build();
        DepoStok hedef = DepoStok.builder().depoId(3L).stokId(2L).miktar(new BigDecimal("0")).build();
        when(depoRepository.findById(1L)).thenReturn(Optional.of(ornekDepo(1L)));
        when(depoRepository.findById(3L)).thenReturn(Optional.of(ornekDepo(3L)));
        when(depoStokRepository.findByDepoIdAndStokId(1L, 2L)).thenReturn(Optional.of(kaynak));
        when(depoStokRepository.findByDepoIdAndStokId(3L, 2L)).thenReturn(Optional.of(hedef));
        when(stokRepository.findAll()).thenReturn(List.of());
        when(depoStokRepository.findByDepoId(1L)).thenReturn(List.of(kaynak));
        when(depoStokRepository.findByDepoId(3L)).thenReturn(List.of(hedef));

        depoService.stokTransfer(1L, 3L, 2L, new BigDecimal("10"));

        assertEquals(0, kaynak.getMiktar().compareTo(new BigDecimal("40")));
        assertEquals(0, hedef.getMiktar().compareTo(new BigDecimal("10")));
    }

    @Test
    void sil_deletes() {
        when(depoRepository.findById(1L)).thenReturn(Optional.of(ornekDepo(1L)));
        depoService.sil(1L);
        verify(depoRepository).deleteById(1L);
    }
}
