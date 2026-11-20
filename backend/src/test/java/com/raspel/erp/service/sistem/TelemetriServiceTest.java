package com.raspel.erp.service.sistem;

import com.raspel.erp.dto.sistem.FrontendHataDTO;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import io.micrometer.core.instrument.MeterRegistry;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TelemetriServiceTest {

    @Mock private TelemetriHizSiniri hizSiniri;
    @Mock private ObjectProvider<MeterRegistry> registryProvider;
    @InjectMocks private TelemetriService service;

    @Test
    void reddedilenIstekSayaciArtirir() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        when(registryProvider.getIfAvailable()).thenReturn(registry);
        when(hizSiniri.izinVar(5L)).thenReturn(false);

        assertFalse(service.kabulEt(5L));

        assertEquals(1.0, registry.get("frontend_errors_total").tag("tur", "reddedilen").counter().count());
    }

    @Test
    void kaydetTuruyleSayacArtirirVeNullAlanlarlaCalisir() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        when(registryProvider.getIfAvailable()).thenReturn(registry);
        FrontendHataDTO dto = FrontendHataDTO.builder().tur("vue").mesaj("boom").build();

        service.kaydet(dto, 1L, 2L);
        service.kaydet(FrontendHataDTO.builder().mesaj("tursuz").build(), null, null);

        assertEquals(1.0, registry.get("frontend_errors_total").tag("tur", "vue").counter().count());
        assertEquals(1.0, registry.get("frontend_errors_total").tag("tur", "bilinmeyen").counter().count());
    }

    @Test
    void uzunStackKirpilirVeHataFirlatmaz() {
        when(registryProvider.getIfAvailable()).thenReturn(new SimpleMeterRegistry());
        StringBuilder stack = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            stack.append("at Katman").append(i).append("(Dosya.java:").append(i).append(")\n");
        }
        FrontendHataDTO dto = FrontendHataDTO.builder()
                .tur("js")
                .mesaj("x".repeat(5000))
                .stack(stack.toString())
                .build();

        assertDoesNotThrow(() -> service.kaydet(dto, 1L, 1L));
    }

    @Test
    void metrikAltyapisiYokkenDeKaydeder() {
        when(registryProvider.getIfAvailable()).thenReturn(null);

        assertDoesNotThrow(() -> service.kaydet(
                FrontendHataDTO.builder().tur("promise").mesaj("hata").build(), 1L, 1L));
    }

    @Test
    void nullGovdeYoksayilir() {
        assertDoesNotThrow(() -> service.kaydet(null, 1L, 1L));
        verifyNoInteractions(registryProvider);
    }
}
