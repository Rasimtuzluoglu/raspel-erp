package com.raspel.erp.service;

import com.raspel.erp.entity.finans.CariHesap;
import com.raspel.erp.entity.sistem.Kullanici;
import com.raspel.erp.exception.BusinessException;
import com.raspel.erp.repository.finans.CariHesapRepository;
import com.raspel.erp.repository.sistem.KullaniciRepository;
import com.raspel.erp.service.sistem.EmailPolitikaService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailPolitikaServiceTest {

    @Mock private CariHesapRepository cariHesapRepository;
    @Mock private KullaniciRepository kullaniciRepository;
    @InjectMocks private EmailPolitikaService service;

    private CariHesap cari(String email) {
        CariHesap c = new CariHesap();
        c.setEmail(email);
        return c;
    }

    @Test
    void aliciDogrula_kayitliCariEpostasiKabulEdilir() {
        when(cariHesapRepository.findBySirketIdOrderByAdAsc(1L)).thenReturn(List.of(cari("musteri@firma.com")));
        when(kullaniciRepository.findBySirketId(eq(1L), any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

        assertDoesNotThrow(() -> service.aliciDogrula("musteri@firma.com", 1L));
    }

    @Test
    void aliciDogrula_hariciEpostaReddedilir() {
        when(cariHesapRepository.findBySirketIdOrderByAdAsc(1L)).thenReturn(List.of(cari("musteri@firma.com")));
        when(kullaniciRepository.findBySirketId(eq(1L), any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

        assertThrows(BusinessException.class, () -> service.aliciDogrula("saldirgan@disari.com", 1L));
    }

    @Test
    void aliciDogrula_kayitliKullaniciEpostasiKabulEdilir() {
        Kullanici k = new Kullanici();
        k.setEmail("personel@firma.com");
        when(cariHesapRepository.findBySirketIdOrderByAdAsc(1L)).thenReturn(List.of());
        when(kullaniciRepository.findBySirketId(eq(1L), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(k)));

        assertDoesNotThrow(() -> service.aliciDogrula("Personel@Firma.com", 1L));
    }

    @Test
    void aliciDogrula_gecersizAdresReddedilir() {
        assertThrows(BusinessException.class, () -> service.aliciDogrula("gecersiz", 1L));
        assertThrows(BusinessException.class, () -> service.aliciDogrula(null, 1L));
    }

    @Test
    void aliciDogrula_sirketYoksaReddedilir() {
        assertThrows(BusinessException.class, () -> service.aliciDogrula("a@b.com", null));
    }
}
