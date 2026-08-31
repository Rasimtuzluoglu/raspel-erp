package com.raspel.erp.service.sistem;

import com.raspel.erp.dto.sistem.OnayAyariDTO;
import com.raspel.erp.entity.sistem.OnayAyari;
import com.raspel.erp.exception.BusinessException;
import com.raspel.erp.repository.sistem.OnayAyariRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OnayAyariServiceTest {

    @Mock
    private OnayAyariRepository onayAyariRepository;
    @Mock
    private com.raspel.erp.config.TenantChecker tenantChecker;

    @InjectMocks
    private OnayAyariService onayAyariService;

    @Test
    void kaydet_gecersizModulHataFirlatir() {
        assertThrows(BusinessException.class, () ->
                onayAyariService.kaydet(1L, OnayAyariDTO.builder().modul("GECERSIZ").build()));
    }

    @Test
    void kaydet_yeniKayitOlusturur() {
        when(onayAyariRepository.findBySirketIdAndModul(1L, "MASRAF")).thenReturn(Optional.empty());
        when(onayAyariRepository.save(any(OnayAyari.class))).thenAnswer(inv -> {
            OnayAyari a = inv.getArgument(0);
            a.setId(1L);
            return a;
        });

        OnayAyariDTO dto = onayAyariService.kaydet(1L, OnayAyariDTO.builder()
                .modul("masraf").esikTutar(new BigDecimal("5000")).otomatikOnay(true).build());

        assertEquals("MASRAF", dto.getModul());
        assertEquals(0, new BigDecimal("5000").compareTo(dto.getEsikTutar()));
        assertTrue(dto.getOtomatikOnay());
    }

    @Test
    void otomatikOnayGecerli_esikAltindaVeAcikIseTrue() {
        when(onayAyariRepository.findBySirketIdAndModul(1L, "MASRAF")).thenReturn(Optional.of(
                OnayAyari.builder().modul("MASRAF").esikTutar(new BigDecimal("1000")).otomatikOnay(true).build()));

        assertTrue(onayAyariService.otomatikOnayGecerli(1L, "MASRAF", new BigDecimal("500")));
    }

    @Test
    void otomatikOnayGecerli_esikUstundeFalse() {
        when(onayAyariRepository.findBySirketIdAndModul(1L, "MASRAF")).thenReturn(Optional.of(
                OnayAyari.builder().modul("MASRAF").esikTutar(new BigDecimal("1000")).otomatikOnay(true).build()));

        assertFalse(onayAyariService.otomatikOnayGecerli(1L, "MASRAF", new BigDecimal("1500")));
    }

    @Test
    void otomatikOnayGecerli_ayarYoksaFalse() {
        when(onayAyariRepository.findBySirketIdAndModul(1L, "MASRAF")).thenReturn(Optional.empty());
        assertFalse(onayAyariService.otomatikOnayGecerli(1L, "MASRAF", new BigDecimal("500")));
    }

    @Test
    void otomatikOnayGecerli_otomatikOnayKapaliIseFalse() {
        when(onayAyariRepository.findBySirketIdAndModul(1L, "MASRAF")).thenReturn(Optional.of(
                OnayAyari.builder().modul("MASRAF").esikTutar(new BigDecimal("1000")).otomatikOnay(false).build()));

        assertFalse(onayAyariService.otomatikOnayGecerli(1L, "MASRAF", new BigDecimal("500")));
    }

    @Test
    void listele_kayitYoksaVarsayilanModulleriDoner() {
        when(onayAyariRepository.findBySirketIdOrderByModulAsc(1L)).thenReturn(java.util.List.of());

        java.util.List<OnayAyariDTO> liste = onayAyariService.listele(1L);

        assertEquals(3, liste.size());
        assertEquals("MASRAF", liste.get(0).getModul());
        assertEquals("SATINALMA", liste.get(1).getModul());
        assertEquals("IZIN", liste.get(2).getModul());
        assertNull(liste.get(0).getId());
        assertEquals(0, liste.get(0).getEsikTutar().compareTo(BigDecimal.ZERO));
    }

    @Test
    void listele_kayitliModulleriKorur() {
        when(onayAyariRepository.findBySirketIdOrderByModulAsc(1L)).thenReturn(java.util.List.of(
                OnayAyari.builder().id(1L).sirketId(1L).modul("MASRAF").esikTutar(new BigDecimal("5000")).otomatikOnay(true).build()));

        java.util.List<OnayAyariDTO> liste = onayAyariService.listele(1L);

        assertEquals(3, liste.size());
        OnayAyariDTO masraf = liste.stream().filter(x -> "MASRAF".equals(x.getModul())).findFirst().orElseThrow();
        assertEquals(1L, masraf.getId());
        assertEquals(0, masraf.getEsikTutar().compareTo(new BigDecimal("5000")));
    }
}
