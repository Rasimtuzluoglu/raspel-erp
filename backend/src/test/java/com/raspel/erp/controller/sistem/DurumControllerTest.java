package com.raspel.erp.controller.sistem;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.actuate.health.HealthComponent;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.health.Status;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DurumControllerTest {

    @Mock
    private HealthEndpoint healthEndpoint;

    @InjectMocks
    private DurumController controller;

    @Test
    void durum_saglikDurumunuVeSurumuDoner() {
        HealthComponent bilesen = mock(HealthComponent.class);
        when(bilesen.getStatus()).thenReturn(Status.UP);
        when(healthEndpoint.health()).thenReturn(bilesen);
        ReflectionTestUtils.setField(controller, "surum", "1.36.0");

        Map<String, Object> govde = controller.durum().getBody();

        assertNotNull(govde);
        assertEquals("UP", govde.get("durum"));
        assertEquals("1.36.0", govde.get("surum"));
        assertNotNull(govde.get("zaman"));
    }

    @Test
    void durum_saglikHatasiDurumundaDownDoner() {
        when(healthEndpoint.health()).thenThrow(new IllegalStateException("db yok"));
        ReflectionTestUtils.setField(controller, "surum", "test");

        Map<String, Object> govde = controller.durum().getBody();

        assertEquals("DOWN", govde.get("durum"));
        assertEquals("test", govde.get("surum"));
    }
}
