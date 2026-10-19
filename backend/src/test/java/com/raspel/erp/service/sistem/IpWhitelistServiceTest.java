package com.raspel.erp.service.sistem;

import com.raspel.erp.entity.sistem.IpWhitelist;
import com.raspel.erp.exception.BusinessException;
import com.raspel.erp.exception.ResourceNotFoundException;
import com.raspel.erp.repository.sistem.IpWhitelistRepository;
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
class IpWhitelistServiceTest {

    @Mock private IpWhitelistRepository ipWhitelistRepository;
    @InjectMocks private IpWhitelistService ipWhitelistService;

    @Test
    void ekle_gecersizIpReddedilir() {
        assertThrows(BusinessException.class, () ->
                ipWhitelistService.ekle(1L, Map.of("ipAdresi", "999.1.1.1")));
        verify(ipWhitelistRepository, never()).save(any());
    }

    @Test
    void ekle_mukerrerIpReddedilir() {
        when(ipWhitelistRepository.existsBySirketIdAndIpAdresi(1L, "10.0.0.1")).thenReturn(true);

        assertThrows(BusinessException.class, () ->
                ipWhitelistService.ekle(1L, Map.of("ipAdresi", "10.0.0.1")));
        verify(ipWhitelistRepository, never()).save(any());
    }

    @Test
    void ekle_cidrKabulEdilir() {
        when(ipWhitelistRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(ipWhitelistRepository.findBySirketIdOrderByIdAsc(1L)).thenReturn(List.of());

        ipWhitelistService.ekle(1L, Map.of("ipAdresi", "192.168.1.0/24", "aciklama", "Ofis"));

        verify(ipWhitelistRepository).save(any(IpWhitelist.class));
    }

    @Test
    void guncelle_durumDegistirir() {
        IpWhitelist kayit = IpWhitelist.builder()
                .id(5L).sirketId(1L).ipAdresi("10.0.0.1").durum("AKTIF").build();
        when(ipWhitelistRepository.findByIdAndSirketId(5L, 1L)).thenReturn(Optional.of(kayit));
        when(ipWhitelistRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(ipWhitelistRepository.findBySirketIdOrderByIdAsc(1L)).thenReturn(List.of(kayit));

        ipWhitelistService.guncelle(1L, 5L, Map.of("durum", "PASIF"));

        assertEquals("PASIF", kayit.getDurum());
    }

    @Test
    void sil_bulunamazsaHataFirlatir() {
        when(ipWhitelistRepository.findByIdAndSirketId(9L, 1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> ipWhitelistService.sil(1L, 9L));
    }
}
