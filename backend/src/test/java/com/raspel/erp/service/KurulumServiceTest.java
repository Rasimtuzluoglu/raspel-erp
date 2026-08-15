package com.raspel.erp.service;

import com.raspel.erp.dto.sistem.KullaniciDTO;
import com.raspel.erp.dto.sistem.KurulumDTO;
import com.raspel.erp.dto.sistem.LoginRequest;
import com.raspel.erp.dto.sistem.LoginResponse;
import com.raspel.erp.entity.sistem.Kullanici;
import com.raspel.erp.entity.sistem.Sirket;
import com.raspel.erp.exception.BusinessException;
import com.raspel.erp.repository.sistem.KullaniciRepository;
import com.raspel.erp.repository.sistem.SirketRepository;
import com.raspel.erp.service.sistem.KullaniciService;
import com.raspel.erp.service.sistem.KurulumService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KurulumServiceTest {

    @Mock private SirketRepository sirketRepository;
    @Mock private KullaniciRepository kullaniciRepository;
    @Mock private KullaniciService kullaniciService;
    @InjectMocks private KurulumService kurulumService;

    private KurulumDTO ornekDTO() {
        return KurulumDTO.builder()
                .ad("Yeni Firma").vergiNo("1234567890").vergiDairesi("Kadıköy VD")
                .adminUsername("yonetici").adminPassword("Sifre1!")
                .adminDisplayName("Yönetici")
                .build();
    }

    @Test
    void durum_kurulumGerekliWhenEmpty() {
        when(sirketRepository.count()).thenReturn(0L);
        var result = kurulumService.durum();
        assertEquals(true, result.get("kurulumGerekli"));
    }

    @Test
    void durum_kurulumTamamWhenCompanyExists() {
        when(sirketRepository.count()).thenReturn(1L);
        var result = kurulumService.durum();
        assertEquals(false, result.get("kurulumGerekli"));
    }

    @Test
    void kurulumYap_createsCompanyAndAdmin() {
        when(sirketRepository.count()).thenReturn(0L);
        when(sirketRepository.save(any(Sirket.class))).thenAnswer(inv -> {
            Sirket s = inv.getArgument(0);
            s.setId(1L);
            return s;
        });
        when(kullaniciService.olustur(any(KullaniciDTO.class)))
                .thenReturn(KullaniciDTO.builder().id(1L).username("yonetici").build());
        Kullanici admin = Kullanici.builder().id(1L).username("yonetici").role("ADMIN").active(true).build();
        when(kullaniciRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(kullaniciService.giris(any(LoginRequest.class)))
                .thenReturn(LoginResponse.builder().id(1L).username("yonetici").girisToken("tok").build());

        var result = kurulumService.kurulumYap(ornekDTO());

        assertNotNull(result);
        assertEquals("yonetici", result.getUsername());
        verify(kullaniciService).olustur(any(KullaniciDTO.class));
        verify(kullaniciService).giris(any(LoginRequest.class));
    }

    @Test
    void kurulumYap_whenAlreadySetup_throws() {
        when(sirketRepository.count()).thenReturn(1L);
        assertThrows(BusinessException.class, () -> kurulumService.kurulumYap(ornekDTO()));
    }
}
