package com.raspel.erp.service.finans;

import com.raspel.erp.exception.BusinessException;
import com.raspel.erp.service.sistem.TcmbKurService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DovizCeviriciTest {

    @Mock
    private TcmbKurService tcmbKurService;

    private DovizCevirici dovizCevirici;

    @BeforeEach
    void kur() {
        dovizCevirici = new DovizCevirici(tcmbKurService);
        dovizCevirici.temizle();
    }

    @org.junit.jupiter.api.AfterEach
    void sonraTemizle() {
        // ThreadLocal statiktir; testler arasi sizmayi onle.
        dovizCevirici.temizle();
    }

    @Test
    void hedefAyarlanmadanHamDoner() {
        assertEquals(0, dovizCevirici.cevir(new BigDecimal("100")).compareTo(new BigDecimal("100")));
    }

    @Test
    void hedefTryIseHamDoner() {
        dovizCevirici.basla("TRY");
        assertEquals(0, dovizCevirici.cevir(new BigDecimal("100")).compareTo(new BigDecimal("100")));
    }

    @Test
    void hedefVarsaCevirir() {
        dovizCevirici.basla("usd");
        when(tcmbKurService.cevir(new BigDecimal("100"), "TRY", "USD")).thenReturn(new BigDecimal("2.1087"));
        assertEquals(0, dovizCevirici.cevir(new BigDecimal("100")).compareTo(new BigDecimal("2.1087")));
        assertEquals("USD", dovizCevirici.hedef());
    }

    @Test
    void kurAlinamazsaHamDoner() {
        dovizCevirici.basla("USD");
        when(tcmbKurService.cevir(new BigDecimal("100"), "TRY", "USD"))
                .thenThrow(new BusinessException("kur yok"));
        assertEquals(0, dovizCevirici.cevir(new BigDecimal("100")).compareTo(new BigDecimal("100")));
    }

    @Test
    void temizleBaglamiSifirlar() {
        dovizCevirici.basla("USD");
        dovizCevirici.temizle();
        assertNull(dovizCevirici.hedef());
    }
}
