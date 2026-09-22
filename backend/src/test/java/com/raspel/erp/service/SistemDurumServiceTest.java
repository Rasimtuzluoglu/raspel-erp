package com.raspel.erp.service;

import com.raspel.erp.repository.sistem.HataLogRepository;
import com.raspel.erp.service.sistem.BackupService;
import com.raspel.erp.service.sistem.DosyaDepolamaService;
import com.raspel.erp.service.sistem.SistemDurumService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
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
    @Mock private DosyaDepolamaService dosyaDepolama;
    @InjectMocks private SistemDurumService sistemDurumService;

    private void adminOlarakGiris() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin", null,
                        List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
    }

    private void userOlarakGiris() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("kullanici", null,
                        List.of(new SimpleGrantedAuthority("ROLE_USER"))));
    }

    @Test
    void durum_admin_fullDetayDoner() {
        adminOlarakGiris();
        try {
            when(healthEndpoint.health()).thenReturn(Health.up().build());
            when(hataLogRepository.count()).thenReturn(0L);
            when(hataLogRepository.findTop50ByOrderByOlusturmaTarihiDesc()).thenReturn(List.of());
            when(backupService.getSchedule()).thenReturn(Map.of("totalBackups", 0));
            when(dosyaDepolama.kullanim()).thenReturn(Map.of("tip", "local"));
            ReflectionTestUtils.setField(sistemDurumService, "surum", "1.6.1");

            Map<String, Object> result = sistemDurumService.durum();

            assertEquals("UP", result.get("durum"));
            assertEquals("1.6.1", result.get("surum"));
            assertEquals(0L, result.get("hataSayisi"));
            assertNotNull(result.get("uptimeMs"));
            assertNotNull(result.get("bellek"));
            assertNotNull(result.get("disk"));
            assertNotNull(result.get("depolama"));
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    void durum_user_hassasAlanlariGormez() {
        userOlarakGiris();
        try {
            when(healthEndpoint.health()).thenReturn(Health.up().build());
            ReflectionTestUtils.setField(sistemDurumService, "surum", "1.6.1");

            Map<String, Object> result = sistemDurumService.durum();

            assertEquals("UP", result.get("durum"));
            assertEquals("1.6.1", result.get("surum"));
            // Hassas altyapı bilgileri USER'a verilmez.
            assertNull(result.get("uptimeMs"));
            assertNull(result.get("bellek"));
            assertNull(result.get("disk"));
            assertNull(result.get("depolama"));
            assertNull(result.get("sonHatalar"));
            verify(hataLogRepository, never()).count();
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}
