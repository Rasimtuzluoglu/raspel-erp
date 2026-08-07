package com.raspel.erp.service.ticaret;

import com.raspel.erp.config.TenantChecker;
import com.raspel.erp.dto.ticaret.CariFirsatDTO;
import com.raspel.erp.entity.ticaret.CariFirsat;
import com.raspel.erp.repository.ticaret.CariFirsatRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import com.raspel.erp.repository.finans.CariHesapRepository;

@ExtendWith(MockitoExtension.class)
class CrmServiceTest {

    @Mock private CariFirsatRepository cariFirsatRepository;
    @Mock private com.raspel.erp.repository.finans.CariHesapRepository cariHesapRepository;
    @Mock private TenantChecker tenantChecker;

    @InjectMocks private CrmService crmService;

    private CariFirsat createFirsat(Long id) {
        return CariFirsat.builder()
                .id(id).ad("Büyük Anlaşma").durum("YENI")
                .deger(BigDecimal.valueOf(50000)).sirketId(1L)
                .build();
    }

    @Test
    void firsatlariGetir_durumFiltreli() {
        when(cariFirsatRepository.findBySirketIdAndDurum(1L, "YENI")).thenReturn(List.of(createFirsat(1L)));
        var result = crmService.firsatlariGetir(1L, "YENI");
        assertEquals(1, result.size());
        assertEquals("Büyük Anlaşma", result.get(0).getAd());
    }

    @Test
    void firsatGetir_bulunamazsaHata() {
        when(cariFirsatRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> crmService.firsatGetir(99L));
    }

    @Test
    void firsatOlustur_varsayilanDurumYeni() {
        CariFirsatDTO dto = CariFirsatDTO.builder().ad("Yeni Fırsat").deger(BigDecimal.valueOf(1000)).build();
        CariFirsat saved = createFirsat(1L);
        saved.setAd("Yeni Fırsat");
        when(cariFirsatRepository.save(any(CariFirsat.class))).thenReturn(saved);
        var result = crmService.firsatOlustur(dto, 1L);
        assertEquals("Yeni Fırsat", result.getAd());
        assertEquals(1L, result.getSirketId());
    }

    @Test
    void firsatGuncelle_durumDegistirir() {
        CariFirsat existing = createFirsat(1L);
        when(cariFirsatRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(cariFirsatRepository.save(any(CariFirsat.class))).thenReturn(existing);
        CariFirsatDTO dto = CariFirsatDTO.builder().durum("KAZANILDI").build();
        var result = crmService.firsatGuncelle(1L, dto);
        assertEquals("KAZANILDI", result.getDurum());
    }

    @Test
    void firsatSil_bulunamazsaHata() {
        when(cariFirsatRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> crmService.firsatSil(99L));
    }
}