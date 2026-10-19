package com.raspel.erp.service.sistem;

import com.raspel.erp.dto.sistem.AnomaliDTO;
import com.raspel.erp.entity.sistem.AnomaliDurum;
import com.raspel.erp.exception.BusinessException;
import com.raspel.erp.repository.sistem.AnomaliDurumRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnomaliDurumServiceTest {

    @Mock private AnomaliDurumRepository anomaliDurumRepository;
    @InjectMocks private AnomaliDurumService anomaliDurumService;

    @Test
    void isaretle_yeniKayitOlusturur() {
        when(anomaliDurumRepository.findBySirketIdAndAnahtar(1L, "a1")).thenReturn(Optional.empty());
        when(anomaliDurumRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        anomaliDurumService.isaretle(1L, "a1", "incelendi", 5L);

        verify(anomaliDurumRepository).save(any(AnomaliDurum.class));
    }

    @Test
    void isaretle_acikKaydiSiler() {
        anomaliDurumService.isaretle(1L, "a1", "ACIK", 5L);

        verify(anomaliDurumRepository).deleteBySirketIdAndAnahtar(1L, "a1");
        verify(anomaliDurumRepository, never()).save(any());
    }

    @Test
    void isaretle_gecersizDurumReddedilir() {
        assertThrows(BusinessException.class, () -> anomaliDurumService.isaretle(1L, "a1", "BELIRSIZ", 5L));
        verify(anomaliDurumRepository, never()).save(any());
    }

    @Test
    void uygula_durumlariTaramaSonucunaIsler() {
        AnomaliDurum d = AnomaliDurum.builder().sirketId(1L).anahtar("a1").durum("YOK_SAYILDI").build();
        when(anomaliDurumRepository.findBySirketId(1L)).thenReturn(List.of(d));

        AnomaliDTO a1 = AnomaliDTO.builder().id("a1").build();
        AnomaliDTO a2 = AnomaliDTO.builder().id("a2").build();
        anomaliDurumService.uygula(1L, List.of(a1, a2));

        assertEquals("YOK_SAYILDI", a1.getDurum());
        assertEquals(null, a2.getDurum());
    }

    @Test
    void durumlariGetir_bosSirketIdBosMapDoner() {
        assertEquals(Map.of(), anomaliDurumService.durumlariGetir(null));
    }
}
