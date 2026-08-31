package com.raspel.erp.service.sistem;

import com.raspel.erp.entity.sistem.Sirket;
import com.raspel.erp.repository.sistem.SirketRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HataBildirimServiceTest {

    @Mock
    private EmailService emailService;
    @Mock
    private SirketRepository sirketRepository;

    @InjectMocks
    private HataBildirimService hataBildirimService;

    @Test
    void hataBildir_alertEmailIleGonderir() {
        ReflectionTestUtils.setField(hataBildirimService, "alertEmail", "admin@test.com");
        hataBildirimService.hataBildir(1L, "500", "NullPointer", "/api/stoklar");
        verify(emailService).emailGonder(eq("admin@test.com"), anyString(), anyString());
    }

    @Test
    void hataBildir_alertEmailYoksaSirketEmailKullanir() {
        ReflectionTestUtils.setField(hataBildirimService, "alertEmail", "");
        Sirket s = Sirket.builder().id(1L).email("sirket@test.com").build();
        when(sirketRepository.findById(1L)).thenReturn(Optional.of(s));

        hataBildirimService.hataBildir(1L, "500", "Hata", "/api/faturalar");

        verify(emailService).emailGonder(eq("sirket@test.com"), anyString(), anyString());
    }

    @Test
    void hataBildir_aliciYoksaGonderilmez() {
        ReflectionTestUtils.setField(hataBildirimService, "alertEmail", "");
        when(sirketRepository.findById(1L)).thenReturn(Optional.of(Sirket.builder().id(1L).build()));

        hataBildirimService.hataBildir(1L, "500", "Hata", "/api/x");

        verify(emailService, never()).emailGonder(anyString(), anyString(), anyString());
    }

    @Test
    void hataBildir_minAraliktanOnceTekrarGonderilmez() {
        ReflectionTestUtils.setField(hataBildirimService, "alertEmail", "admin@test.com");

        hataBildirimService.hataBildir(1L, "500", "Hata1", "/api/a");
        hataBildirimService.hataBildir(1L, "500", "Hata2", "/api/b");

        verify(emailService, times(1)).emailGonder(anyString(), anyString(), anyString());
    }

    @Test
    void hataBildir_nullAlanlardaGuvenliCalisir() {
        ReflectionTestUtils.setField(hataBildirimService, "alertEmail", "admin@test.com");
        hataBildirimService.hataBildir(null, null, null, null);
        verify(emailService).emailGonder(eq("admin@test.com"), anyString(), anyString());
    }
}
