package com.raspel.erp.service.envanter;

import com.raspel.erp.dto.envanter.UretimHatDTO;
import com.raspel.erp.entity.envanter.UretimHat;
import com.raspel.erp.exception.BusinessException;
import com.raspel.erp.repository.envanter.UretimEmriRepository;
import com.raspel.erp.repository.envanter.UretimHatRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UretimHatServiceTest {

    @Mock private UretimHatRepository hatRepository;
    @Mock private UretimEmriRepository emirRepository;

    @InjectMocks private UretimHatService hatService;

    private UretimHat hat(Long id, String ad, int kapasite) {
        return UretimHat.builder().id(id).sirketId(1L).ad(ad).kapasite(kapasite).aktif(true).build();
    }

    @Test
    void listele_dolulukVeGecikenSayisiniHesaplar() {
        when(hatRepository.findBySirketIdOrderByAdAsc(1L))
                .thenReturn(List.of(hat(1L, "Hat A", 4), hat(2L, "Hat B", 2)));
        when(emirRepository.countAktifByHat(1L, 1L)).thenReturn(2L);
        when(emirRepository.countGecikenByHat(eq(1L), eq(1L), any())).thenReturn(1L);
        when(emirRepository.countAktifByHat(1L, 2L)).thenReturn(5L);
        when(emirRepository.countGecikenByHat(eq(1L), eq(2L), any())).thenReturn(0L);

        List<UretimHatDTO> liste = hatService.listele(1L);

        assertEquals(2, liste.size());
        assertEquals(50, liste.get(0).getDolulukYuzde());
        assertEquals(1L, liste.get(0).getGecikenEmir());
        // Kapasite aşılsa da doluluk %100 ile sınırlanır.
        assertEquals(100, liste.get(1).getDolulukYuzde());
    }

    @Test
    void olustur_ayniAdiReddeder() {
        when(hatRepository.existsBySirketIdAndAdIgnoreCase(1L, "Hat A")).thenReturn(true);

        assertThrows(BusinessException.class,
                () -> hatService.olustur(UretimHatDTO.builder().ad("Hat A").build(), 1L));
        verify(hatRepository, never()).save(any());
    }

    @Test
    void olustur_varsayilanKapasiteVeAktiflikAtar() {
        when(hatRepository.existsBySirketIdAndAdIgnoreCase(1L, "Hat C")).thenReturn(false);
        when(hatRepository.save(any())).thenAnswer(inv -> {
            UretimHat h = inv.getArgument(0);
            h.setId(3L);
            return h;
        });

        UretimHatDTO dto = hatService.olustur(UretimHatDTO.builder().ad("  Hat C  ").build(), 1L);

        assertEquals("Hat C", dto.getAd());
        assertEquals(5, dto.getKapasite());
        assertTrue(dto.getAktif());
        assertEquals(0, dto.getDolulukYuzde());
    }

    @Test
    void sil_bagliEmirVarsaReddeder() {
        when(hatRepository.findByIdAndSirketId(1L, 1L)).thenReturn(Optional.of(hat(1L, "Hat A", 3)));
        when(emirRepository.existsBySirketIdAndHatId(1L, 1L)).thenReturn(true);

        assertThrows(BusinessException.class, () -> hatService.sil(1L, 1L));
        verify(hatRepository, never()).delete(any());
    }

    @Test
    void sil_bagliEmirYoksaSiler() {
        UretimHat h = hat(1L, "Hat A", 3);
        when(hatRepository.findByIdAndSirketId(1L, 1L)).thenReturn(Optional.of(h));
        when(emirRepository.existsBySirketIdAndHatId(1L, 1L)).thenReturn(false);

        hatService.sil(1L, 1L);

        verify(hatRepository).delete(h);
    }

    @Test
    void guncelle_baskaHattinAdiniReddeder() {
        when(hatRepository.findByIdAndSirketId(1L, 1L)).thenReturn(Optional.of(hat(1L, "Hat A", 3)));
        when(hatRepository.findBySirketIdOrderByAdAsc(1L)).thenReturn(List.of(hat(1L, "Hat A", 3), hat(2L, "Hat B", 3)));

        assertThrows(BusinessException.class,
                () -> hatService.guncelle(1L, UretimHatDTO.builder().ad("Hat B").build(), 1L));
    }
}
