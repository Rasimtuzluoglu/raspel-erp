package com.raspel.erp.service.sube;

import com.raspel.erp.entity.sube.Depo;
import com.raspel.erp.entity.sube.DepoStok;
import com.raspel.erp.repository.sube.DepoRepository;
import com.raspel.erp.repository.sube.DepoStokRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DepoStokServiceTest {

    @Mock private DepoStokRepository depoStokRepository;
    @Mock private DepoRepository depoRepository;
    @InjectMocks private DepoStokService service;

    @Test
    void guncelle_varolanStoguArtirir() {
        DepoStok mevcut = DepoStok.builder().depoId(1L).stokId(2L).miktar(new BigDecimal("10")).build();
        when(depoStokRepository.findByDepoIdAndStokIdForUpdate(1L, 2L)).thenReturn(Optional.of(mevcut));

        service.guncelle(1L, 2L, new BigDecimal("5"));

        ArgumentCaptor<DepoStok> captor = ArgumentCaptor.forClass(DepoStok.class);
        verify(depoStokRepository).save(captor.capture());
        assertEquals(0, captor.getValue().getMiktar().compareTo(new BigDecimal("15")));
    }

    @Test
    void guncelle_yoksaYeniKayitOlusturur() {
        when(depoStokRepository.findByDepoIdAndStokIdForUpdate(1L, 2L)).thenReturn(Optional.empty());

        service.guncelle(1L, 2L, new BigDecimal("7"));

        ArgumentCaptor<DepoStok> captor = ArgumentCaptor.forClass(DepoStok.class);
        verify(depoStokRepository).save(captor.capture());
        assertEquals(0, captor.getValue().getMiktar().compareTo(new BigDecimal("7")));
    }

    @Test
    void guncelle_negatifeDuserseksiZeroyaCeker() {
        DepoStok mevcut = DepoStok.builder().depoId(1L).stokId(2L).miktar(new BigDecimal("3")).build();
        when(depoStokRepository.findByDepoIdAndStokIdForUpdate(1L, 2L)).thenReturn(Optional.of(mevcut));

        service.guncelle(1L, 2L, new BigDecimal("-10"));

        ArgumentCaptor<DepoStok> captor = ArgumentCaptor.forClass(DepoStok.class);
        verify(depoStokRepository).save(captor.capture());
        assertEquals(0, captor.getValue().getMiktar().compareTo(BigDecimal.ZERO));
    }

    @Test
    void guncelle_gecersizParametrelerdeHicbirSeyYapmaz() {
        service.guncelle(null, 2L, new BigDecimal("5"));
        service.guncelle(1L, null, new BigDecimal("5"));
        service.guncelle(1L, 2L, null);
        service.guncelle(1L, 2L, BigDecimal.ZERO);

        verifyNoInteractions(depoStokRepository);
    }

    @Test
    void varsayilanDepoId_enKucukIdliAktifDepoyuDoner() {
        Depo d1 = Depo.builder().id(7L).build();
        Depo d2 = Depo.builder().id(3L).build();
        when(depoRepository.findBySirketIdAndAktifTrue(5L)).thenReturn(List.of(d1, d2));

        assertEquals(3L, service.varsayilanDepoId(5L));
    }

    @Test
    void varsayilanDepoId_depoYoksaNullDoner() {
        when(depoRepository.findBySirketIdAndAktifTrue(5L)).thenReturn(List.of());
        assertNull(service.varsayilanDepoId(5L));
        assertNull(service.varsayilanDepoId(null));
    }

    @Test
    void coz_verilenDepoyuOncelerYoksaVarsayilanaDuser() {
        when(depoRepository.findById(9L)).thenReturn(Optional.of(Depo.builder().id(9L).sirketId(5L).build()));
        assertEquals(9L, service.coz(9L, 5L));

        when(depoRepository.findBySirketIdAndAktifTrue(5L)).thenReturn(List.of(Depo.builder().id(4L).build()));
        assertEquals(4L, service.coz(null, 5L));
    }

    @Test
    void coz_baskaSirketinDeposuReddedilir() {
        when(depoRepository.findById(9L)).thenReturn(Optional.of(Depo.builder().id(9L).sirketId(6L).build()));
        assertThrows(com.raspel.erp.exception.ResourceNotFoundException.class, () -> service.coz(9L, 5L));
    }

    @Test
    void coz_olmayanDepoReddedilir() {
        when(depoRepository.findById(9L)).thenReturn(Optional.empty());
        assertThrows(com.raspel.erp.exception.ResourceNotFoundException.class, () -> service.coz(9L, 5L));
    }
}
