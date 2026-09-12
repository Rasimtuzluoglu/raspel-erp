package com.raspel.erp.service;

import com.raspel.erp.dto.sistem.PushAbonelikDTO;
import com.raspel.erp.entity.sistem.PushAbonelik;
import com.raspel.erp.exception.BusinessException;
import com.raspel.erp.repository.sistem.PushAbonelikRepository;
import com.raspel.erp.service.sistem.WebPushService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebPushServiceTest {

    @Mock private PushAbonelikRepository pushAbonelikRepository;
    @Mock private com.raspel.erp.repository.sistem.KullaniciRepository kullaniciRepository;
    @InjectMocks private WebPushService webPushService;

    private PushAbonelikDTO dto(String endpoint) {
        return PushAbonelikDTO.builder()
                .endpoint(endpoint)
                .keys(PushAbonelikDTO.Keys.builder().p256dh("p256").auth("auth").build())
                .build();
    }

    @Test
    void aboneOl_yeniAbonelikKaydeder() {
        when(pushAbonelikRepository.findByEndpoint("https://push/x")).thenReturn(Optional.empty());
        when(pushAbonelikRepository.save(any(PushAbonelik.class))).thenAnswer(inv -> inv.getArgument(0));

        webPushService.aboneOl(dto("https://push/x"), 5L, 1L, "UA");

        ArgumentCaptor<PushAbonelik> captor = ArgumentCaptor.forClass(PushAbonelik.class);
        verify(pushAbonelikRepository).save(captor.capture());
        assertEquals(5L, captor.getValue().getKullaniciId());
        assertEquals(1L, captor.getValue().getSirketId());
        assertEquals("p256", captor.getValue().getP256dh());
    }

    @Test
    void aboneOl_mevcutAbonelikGunceller() {
        PushAbonelik mevcut = PushAbonelik.builder().id(9L).endpoint("https://push/x").build();
        when(pushAbonelikRepository.findByEndpoint("https://push/x")).thenReturn(Optional.of(mevcut));
        when(pushAbonelikRepository.save(any(PushAbonelik.class))).thenAnswer(inv -> inv.getArgument(0));

        webPushService.aboneOl(dto("https://push/x"), 5L, 1L, "UA");

        assertEquals(9L, mevcut.getId());
        assertEquals("auth", mevcut.getAuth());
    }

    @Test
    void aboneOl_gecersizVeriHataFirlatir() {
        assertThrows(BusinessException.class, () -> webPushService.aboneOl(
                PushAbonelikDTO.builder().endpoint("e").build(), 1L, 1L, null));
    }

    @Test
    void aboneSil_endpointIleSiler() {
        webPushService.aboneSil("https://push/x");
        verify(pushAbonelikRepository).deleteByEndpoint("https://push/x");
    }

    @Test
    void gonder_aktifDegilseSifirDoner() {
        int sonuc = webPushService.gonder(1L, "b", "m", "/");
        assertEquals(0, sonuc);
        verifyNoInteractions(pushAbonelikRepository);
    }

    @Test
    void aktif_anahtarYoksaFalse() {
        assertFalse(webPushService.aktif());
    }

    @Test
    void turIzinli_bosTercihTumTiplereAciKTir() {
        assertTrue(WebPushService.turIzinli(null, "STOK"));
        assertTrue(WebPushService.turIzinli("", "STOK"));
        assertTrue(WebPushService.turIzinli("FATURA", null));
    }

    @Test
    void turIzinli_whitelistUygular() {
        assertTrue(WebPushService.turIzinli("FATURA,SIPARIS", "siparis"));
        assertFalse(WebPushService.turIzinli("FATURA,SIPARIS", "STOK"));
    }
}
