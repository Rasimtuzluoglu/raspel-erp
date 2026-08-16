package com.raspel.erp.service;

import com.raspel.erp.repository.sistem.HataLogRepository;
import com.raspel.erp.service.sistem.BackupService;
import com.raspel.erp.service.sistem.SistemDurumService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SistemDurumServiceTest {

    @Mock private HealthEndpoint healthEndpoint;
    @Mock private HataLogRepository hataLogRepository;
    @Mock private BackupService backupService;
    @InjectMocks private SistemDurumService sistemDurumService;

    @Test
    void durum_returnsUpStatus() {
        when(healthEndpoint.health()).thenReturn(Health.up().build());
        when(hataLogRepository.count()).thenReturn(0L);
        when(hataLogRepository.findTop50ByOrderByOlusturmaTarihiDesc()).thenReturn(List.of());
        when(backupService.getSchedule()).thenReturn(Map.of("totalBackups", 0));
        ReflectionTestUtils.setField(sistemDurumService, "surum", "1.6.1");

        Map<String, Object> result = sistemDurumService.durum();

        assertEquals("UP", result.get("durum"));
        assertEquals("1.6.1", result.get("surum"));
        assertEquals(0L, result.get("hataSayisi"));
        assertNotNull(result.get("uptimeMs"));
        assertNotNull(result.get("bellek"));
        assertNotNull(result.get("disk"));
    }
}
