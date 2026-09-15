package com.raspel.erp.service.sistem;

import com.raspel.erp.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import javax.sql.DataSource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class BackupServiceTest {

    @TempDir
    Path tempDir;

    private BackupService backupService;

    @BeforeEach
    void setUp() {
        DataSource dataSource = mock(DataSource.class);
        DosyaDepolamaService dosyaDepolama = mock(DosyaDepolamaService.class);
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        backupService = new BackupService(dataSource, dosyaDepolama, jdbcTemplate);
        ReflectionTestUtils.setField(backupService, "backupDir", tempDir.toString());
        ReflectionTestUtils.setField(backupService, "dbHost", "localhost");
        ReflectionTestUtils.setField(backupService, "dbPort", "5432");
        ReflectionTestUtils.setField(backupService, "dbName", "raspelerp");
        ReflectionTestUtils.setField(backupService, "dbPasswordProperty", "");
        ReflectionTestUtils.setField(backupService, "retentionDays", 30);
        ReflectionTestUtils.setField(backupService, "autoCron", "0 0 3 * * ?");
        backupService.init();
    }

    @Test
    void listBackups_bosDizindeBosListeDoner() {
        List<Map<String, Object>> sonuc = backupService.listBackups();
        assertTrue(sonuc.isEmpty());
    }

    @Test
    void parseType_dogruTurleriCozumler() {
        String daily = "raspelerp_DAILY_20260816_030000.sql.gz";
        String weekly = "raspelerp_WEEKLY_20260816_030000.sql.gz";
        String legacy = "raspelerp-DAILY-20260816-030000.sql.gz";
        assertEquals("DAILY", ReflectionTestUtils.invokeMethod(backupService, "parseType", daily));
        assertEquals("WEEKLY", ReflectionTestUtils.invokeMethod(backupService, "parseType", weekly));
        assertEquals("DAILY", ReflectionTestUtils.invokeMethod(backupService, "parseType", legacy));
        assertEquals("DAILY", ReflectionTestUtils.invokeMethod(backupService, "parseType", "bilinmeyen.sql.gz"));
    }

    @Test
    void downloadBackup_olmayanDosyaIcinHataFirlatir() {
        assertThrows(RuntimeException.class, () -> backupService.downloadBackup("raspelerp_DAILY_yok.sql.gz"));
    }

    @Test
    void downloadBackup_pathTraversalReddedilir() {
        assertThrows(RuntimeException.class, () -> backupService.downloadBackup("../etc/passwd"));
    }

    @Test
    void restoreBackup_pathTraversalReddedilir() {
        assertThrows(RuntimeException.class, () -> backupService.restoreBackup("../etc/passwd"));
    }

    @Test
    void restoreBackup_olmayanDosyaIcinHataFirlatir() {
        assertThrows(RuntimeException.class, () -> backupService.restoreBackup("raspelerp_DAILY_yok.sql.gz"));
    }

    @Test
    void deleteBackup_olmayanDosyaIcinHataFirlatir() {
        assertThrows(RuntimeException.class, () -> backupService.deleteBackup("raspelerp_DAILY_yok.sql.gz"));
    }

    @Test
    void deleteBackup_mevcutDosyayiSiler() throws Exception {
        Path dosya = tempDir.resolve("raspelerp_DAILY_20260816_030000.sql.gz");
        Files.writeString(dosya, "test");

        backupService.deleteBackup("raspelerp_DAILY_20260816_030000.sql.gz");

        assertFalse(Files.exists(dosya));
    }

    @Test
    void syncToCloud_bulutAktifDegilkenHataFirlatir() {
        ReflectionTestUtils.setField(backupService, "cloudEnabled", false);
        assertThrows(BusinessException.class, () -> backupService.syncToCloud("raspelerp_DAILY_x.sql.gz"));
    }

    @Test
    void getSchedule_varsayilanRetentionDoner() {
        Map<String, Object> schedule = backupService.getSchedule();
        assertNotNull(schedule.get("retention"));
        @SuppressWarnings("unchecked")
        Map<String, Integer> retention = (Map<String, Integer>) schedule.get("retention");
        assertEquals(30, retention.get("DAILY"));
        assertEquals(180, retention.get("WEEKLY"));
        assertEquals(365, retention.get("MONTHLY"));
        assertEquals(-1, retention.get("YEARLY"));
        assertEquals(0, schedule.get("totalBackups"));
    }

    @Test
    void getCloudConfig_devreDisiBaslar() {
        Map<String, Object> config = backupService.getCloudConfig();
        assertEquals("DEVRE_DISI", config.get("status"));
        assertEquals(false, config.get("enabled"));
    }
}