package com.raspel.erp.service.sistem;

import com.raspel.erp.exception.BusinessException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.sql.DataSource;
import java.io.*;
import java.nio.file.*;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;
import com.raspel.erp.entity.sistem.Not;

@Service
@Slf4j
public class BackupService {

    private static final Map<String, Integer> RETENTION_MAP = Map.of(
            "DAILY", 30,
            "WEEKLY", 180,
            "MONTHLY", 365,
            "YEARLY", -1
    );

    private static final List<String> TYPE_ORDER = List.of("YEARLY", "MONTHLY", "WEEKLY", "DAILY");

    @Value("${app.backup.dir:/app/backups}")
    private String backupDir;

    @Value("${app.backup.db-host:localhost}")
    private String dbHost;

    @Value("${app.backup.db-port:5432}")
    private String dbPort;

    @Value("${app.backup.db-name:raspelerp}")
    private String dbName;

    @Value("${app.backup.db-password:}")
    private String dbPasswordProperty;

    @Value("${app.backup.cloud-enabled:false}")
    private boolean cloudEnabled;

    @Value("${app.backup.cloud-endpoint:}")
    private String cloudEndpoint;

    private Path backupPath;
    private Path schedulePath;

    private final DataSource dataSource;
    private final RestTemplate restTemplate;

    public BackupService(DataSource dataSource, RestTemplate restTemplate) {
        this.dataSource = dataSource;
        this.restTemplate = restTemplate;
    }

    @PostConstruct
    public void init() {
        backupPath = Path.of(backupDir);
        schedulePath = backupPath.resolve("schedule.json");
        try {
            Files.createDirectories(backupPath);
        } catch (IOException e) {
            log.error("Backup directory could not be created: {}", backupDir, e);
        }
        log.info("BackupService initialized. Dir: {}", backupDir);
    }

    public String manualBackup(String type) {
        if (type == null || !RETENTION_MAP.containsKey(type)) type = "DAILY";
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        String filename = "raspelerp-" + type + "-" + timestamp + ".sql.gz";
        Path outputFile = backupPath.resolve(filename);

        String dbUser = System.getenv("DB_USERNAME");
        if (dbUser == null) dbUser = "postgres";
        String dbPass = dbPasswordProperty;
        if (dbPass == null || dbPass.isBlank()) {
            dbPass = System.getenv("DB_PASSWORD");
        }
        if (dbPass == null) dbPass = "";

        try {
            ProcessBuilder pb = new ProcessBuilder(
                    "pg_dump",
                    "-h", dbHost,
                    "-p", dbPort,
                    "-U", dbUser,
                    "-d", dbName,
                    "--no-owner",
                    "--no-acl",
                    "--clean",
                    "--if-exists"
            );
            pb.environment().put("PGPASSWORD", dbPass);

            Process dump = pb.start();
            ProcessBuilder gzip = new ProcessBuilder("gzip", "-f");
            File gzipOut = outputFile.toFile();
            gzip.redirectOutput(gzipOut);
            Process gzipProc = gzip.start();

            try (InputStream dumpOut = dump.getInputStream();
                 OutputStream gzipIn = gzipProc.getOutputStream()) {
                dumpOut.transferTo(gzipIn);
            }

            int dumpExit = dump.waitFor();
            gzipProc.getOutputStream().close();
            gzipProc.waitFor();

            if (dumpExit != 0) {
                try (InputStream err = dump.getErrorStream()) {
                    String error = new String(err.readAllBytes());
                    log.error("pg_dump failed: {}", error);
                    throw new RuntimeException("pg_dump failed: " + error);
                }
            }

            long size = Files.size(outputFile);
            log.info("Backup created: {} ({} bytes, type={})", filename, size, type);
            return filename;
        } catch (Exception e) {
            log.error("Backup failed", e);
            throw new RuntimeException("Backup failed: " + e.getMessage());
        }
    }

    public List<Map<String, Object>> listBackups() {
        List<Map<String, Object>> list = new ArrayList<>();
        try {
            List<Path> files = Files.list(backupPath)
                    .filter(f -> f.getFileName().toString().startsWith("raspelerp-") && !f.getFileName().toString().equals("schedule.json"))
                    .sorted(Comparator.reverseOrder())
                    .collect(Collectors.toList());

            for (Path f : files) {
                Map<String, Object> item = new LinkedHashMap<>();
                String name = f.getFileName().toString();
                item.put("filename", name);
                item.put("size", Files.size(f));
                item.put("lastModified", Files.getLastModifiedTime(f).toMillis());
                item.put("type", parseType(name));
                list.add(item);
            }
        } catch (IOException e) {
            log.error("Failed to list backups", e);
        }
        return list;
    }

    public byte[] downloadBackup(String filename) {
        Path file = backupPath.resolve(filename);
        if (!Files.exists(file) || !file.normalize().startsWith(backupPath)) {
            throw new RuntimeException("Backup file not found: " + filename);
        }
        try {
            return Files.readAllBytes(file);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read backup file: " + filename, e);
        }
    }

    public void deleteBackup(String filename) {
        Path file = backupPath.resolve(filename);
        if (!Files.exists(file) || !file.normalize().startsWith(backupPath)) {
            throw new RuntimeException("Backup file not found: " + filename);
        }
        try {
            Files.delete(file);
            log.info("Backup deleted: {}", filename);
        } catch (IOException e) {
            throw new RuntimeException("Failed to delete backup: " + filename, e);
        }
    }

    /**
     * Bir yedek dosyasını geri yükler (gunzip | psql). Dosya .sql.gz formatında olmalıdır.
     */
    public String restoreBackup(String filename) {
        Path file = backupPath.resolve(filename).normalize();
        if (!file.startsWith(backupPath) || !Files.exists(file)) {
            throw new RuntimeException("Backup file not found: " + filename);
        }

        String dbUser = System.getenv("DB_USERNAME");
        if (dbUser == null) dbUser = "postgres";
        String dbPass = dbPasswordProperty;
        if (dbPass == null || dbPass.isBlank()) {
            dbPass = System.getenv("DB_PASSWORD");
        }
        if (dbPass == null) dbPass = "";

        try {
            ProcessBuilder psql = new ProcessBuilder(
                    "psql",
                    "-h", dbHost,
                    "-p", dbPort,
                    "-U", dbUser,
                    "-d", dbName,
                    "--set", "ON_ERROR_STOP=1"
            );
            psql.environment().put("PGPASSWORD", dbPass);
            Process psqlProc = psql.start();

            ProcessBuilder gunzip = new ProcessBuilder("gunzip", "-c", file.toString());
            Process gunzipProc = gunzip.start();

            try (InputStream gunzipOut = gunzipProc.getInputStream();
                 OutputStream psqlIn = psqlProc.getOutputStream()) {
                gunzipOut.transferTo(psqlIn);
            }
            psqlProc.getOutputStream().close();

            int gunzipExit = gunzipProc.waitFor();
            int psqlExit = psqlProc.waitFor();
            if (gunzipExit != 0 || psqlExit != 0) {
                try (InputStream err = psqlProc.getErrorStream()) {
                    String error = new String(err.readAllBytes());
                    log.error("Restore failed: {}", error);
                    throw new RuntimeException("Restore failed: " + error);
                }
            }
            log.info("Backup restored: {}", filename);
            return filename;
        } catch (Exception e) {
            log.error("Restore failed", e);
            throw new RuntimeException("Restore failed: " + e.getMessage());
        }
    }

    /**
     * Çoklu instance ortamında aynı anda yalnızca bir örneğin yedek almasını
     * sağlar (PostgreSQL advisory lock). Kilit alınamazsa görev atlanır.
     */
    private boolean yedekKilidiniAl() {
        try (Connection conn = dataSource.getConnection();
             Statement st = conn.createStatement()) {
            try (ResultSet rs = st.executeQuery("SELECT pg_try_advisory_lock(72495631)")) {
                if (rs.next() && rs.getBoolean(1)) {
                    log.info("Yedekleme kilidi alındı");
                    return true;
                }
            }
            log.warn("Yedekleme kilidi başka bir instance tarafından tutuluyor, görev atlanıyor");
            return false;
        } catch (Exception e) {
            log.warn("Yedekleme kilidi alınamadı (tek instance varsayılıyor): {}", e.getMessage());
            return true;
        }
    }

    @Scheduled(cron = "0 0 3 * * ?")
    public void dailyAutoBackup() {
        if (!yedekKilidiniAl()) return;
        log.info("Daily auto backup started");
        try {
            manualBackup("DAILY");
            cleanOldBackups("DAILY", RETENTION_MAP.get("DAILY"));
        } catch (Exception e) {
            log.error("Daily auto backup failed", e);
        }
    }

    @Scheduled(cron = "0 0 3 ? * SUN")
    public void weeklyAutoBackup() {
        if (!yedekKilidiniAl()) return;
        log.info("Weekly auto backup started");
        try {
            manualBackup("WEEKLY");
            cleanOldBackups("WEEKLY", RETENTION_MAP.get("WEEKLY"));
        } catch (Exception e) {
            log.error("Weekly auto backup failed", e);
        }
    }

    @Scheduled(cron = "0 0 3 1 * ?")
    public void monthlyAutoBackup() {
        if (!yedekKilidiniAl()) return;
        log.info("Monthly auto backup started");
        try {
            manualBackup("MONTHLY");
            cleanOldBackups("MONTHLY", RETENTION_MAP.get("MONTHLY"));
        } catch (Exception e) {
            log.error("Monthly auto backup failed", e);
        }
    }

    @Scheduled(cron = "0 0 3 1 1 ?")
    public void yearlyAutoBackup() {
        if (!yedekKilidiniAl()) return;
        log.info("Yearly auto backup started");
        try {
            manualBackup("YEARLY");
        } catch (Exception e) {
            log.error("Yearly auto backup failed", e);
        }
    }

    private void cleanOldBackups(String type, int retentionDays) {
        if (retentionDays < 0) return;
        long cutoff = System.currentTimeMillis() - retentionDays * 86400000L;
        try {
            List<Path> oldFiles = Files.list(backupPath)
                    .filter(f -> f.getFileName().toString().startsWith("raspelerp-" + type + "-"))
                    .filter(f -> {
                        try { return Files.getLastModifiedTime(f).toMillis() < cutoff; }
                        catch (IOException e) { return false; }
                    })
                    .collect(Collectors.toList());

            for (Path f : oldFiles) {
                Files.delete(f);
                log.info("Old {} backup deleted: {}", type, f.getFileName());
            }
            if (!oldFiles.isEmpty()) {
                log.info("Cleaned {} old {} backup(s)", oldFiles.size(), type);
            }
        } catch (IOException e) {
            log.error("Failed to clean old {} backups", type, e);
        }
    }

    public Map<String, Object> getSchedule() {
        Map<String, Object> schedule = new LinkedHashMap<>();
        schedule.put("retention", Map.of(
                "DAILY", RETENTION_MAP.get("DAILY"),
                "WEEKLY", RETENTION_MAP.get("WEEKLY"),
                "MONTHLY", RETENTION_MAP.get("MONTHLY"),
                "YEARLY", RETENTION_MAP.get("YEARLY")
        ));
        schedule.put("lastBackup", getLastBackupTime());
        schedule.put("totalBackups", listBackups().size());
        schedule.put("totalSize", getTotalBackupSize());
        schedule.put("counts", getTypeCounts());
        return schedule;
    }

    /**
     * Yedek doğrulama health-check'i. Son yedeğin varlığını, bütünlüğünü (gzip testi)
     * ve güncelliğini kontrol eder. Durum: OK, UYARI veya KRITIK.
     */
    public Map<String, Object> yedekDogrula() {
        Map<String, Object> sonuc = new LinkedHashMap<>();
        List<Map<String, Object>> backups = listBackups();
        if (backups.isEmpty()) {
            sonuc.put("durum", "KRITIK");
            sonuc.put("mesaj", "Hiç yedek bulunamadı");
            sonuc.put("toplamYedek", 0);
            return sonuc;
        }
        Map<String, Object> latest = backups.get(0);
        String filename = (String) latest.get("filename");
        long size = ((Number) latest.get("size")).longValue();
        long lastModified = ((Number) latest.get("lastModified")).longValue();
        long yasSaat = (System.currentTimeMillis() - lastModified) / 3_600_000L;

        boolean butunluk = gunzipDogrula(filename);

        String durum;
        if (!butunluk || size == 0) {
            durum = "KRITIK";
        } else if (yasSaat > 168) {
            durum = "KRITIK";
        } else if (yasSaat > 48) {
            durum = "UYARI";
        } else {
            durum = "OK";
        }

        sonuc.put("durum", durum);
        sonuc.put("sonYedek", filename);
        sonuc.put("boyut", size);
        sonuc.put("yasSaat", yasSaat);
        sonuc.put("butunluk", butunluk);
        sonuc.put("toplamYedek", backups.size());
        return sonuc;
    }

    private boolean gunzipDogrula(String filename) {
        Path file = backupPath.resolve(filename);
        try (InputStream is = Files.newInputStream(file);
             GZIPInputStream gz = new GZIPInputStream(is)) {
            byte[] buf = new byte[8192];
            long total = 0;
            int n;
            while ((n = gz.read(buf)) != -1) total += n;
            return total > 0;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Günlük yedek doğrulama health-check'i. Son yedek bayat ya da bozuksa uyarı loglar.
     * (Gerçek bir "restore" testi yapmaz; bütünlük + güncellik kontrolü yapar.)
     */
    @Scheduled(cron = "0 30 4 * * *")
    public void gunlukYedekDogrulama() {
        try {
            Map<String, Object> sonuc = yedekDogrula();
            String durum = String.valueOf(sonuc.get("durum"));
            if ("KRITIK".equals(durum) || "UYARI".equals(durum)) {
                log.warn("Yedek doğrulama uyarısı: durum={}, sonYedek={}, yasSaat={}",
                        durum, sonuc.get("sonYedek"), sonuc.get("yasSaat"));
            } else {
                log.info("Yedek doğrulama OK: sonYedek={}", sonuc.get("sonYedek"));
            }
        } catch (Exception e) {
            log.warn("Yedek doğrulama çalıştırılamadı: {}", e.getMessage());
        }
    }

    public void cleanAllOldBackups() {
        for (var entry : RETENTION_MAP.entrySet()) {
            if (entry.getValue() > 0) {
                cleanOldBackups(entry.getKey(), entry.getValue());
            }
        }
    }

    private String parseType(String filename) {
        for (String t : TYPE_ORDER) {
            if (filename.contains("-" + t + "-")) return t;
        }
        return "DAILY";
    }

    private String getLastBackupTime() {
        try {
            Optional<Path> latest = Files.list(backupPath)
                    .filter(f -> f.getFileName().toString().startsWith("raspelerp-") && !f.getFileName().toString().equals("schedule.json"))
                    .max(Comparator.comparing(f -> {
                        try { return Files.getLastModifiedTime(f); }
                        catch (IOException e) { return null; }
                    }));
            if (latest.isPresent()) {
                return Files.getLastModifiedTime(latest.get()).toString();
            }
        } catch (IOException ignored) {}
        return null;
    }

    private long getTotalBackupSize() {
        try {
            return Files.list(backupPath)
                    .filter(f -> f.getFileName().toString().startsWith("raspelerp-") && !f.getFileName().toString().equals("schedule.json"))
                    .mapToLong(f -> {
                        try { return Files.size(f); }
                        catch (IOException e) { return 0; }
                    })
                    .sum();
        } catch (IOException e) {
            return 0;
        }
    }

    private Map<String, Long> getTypeCounts() {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (String t : TYPE_ORDER) counts.put(t, 0L);
        try {
            Files.list(backupPath)
                    .filter(f -> f.getFileName().toString().startsWith("raspelerp-"))
                    .forEach(f -> {
                        String t = parseType(f.getFileName().toString());
                        counts.merge(t, 1L, Long::sum);
                    });
        } catch (IOException ignored) {}
        return counts;
    }

    private static final Map<String, Object> CLOUD_CONFIG = new HashMap<>(Map.of(
            "provider", "MANUEL",
            "bucketName", "",
            "region", "",
            "autoSync", false,
            "encryptionEnabled", false,
            "encryptionAlgorithm", "AES-256",
            "lastSyncTime", "",
            "status", "DEVRE_DISI"
    ));

    public Map<String, Object> getCloudConfig() {
        Map<String, Object> config = new HashMap<>(CLOUD_CONFIG);
        config.put("status", cloudEnabled ? "AKTIF" : "DEVRE_DISI");
        config.put("enabled", cloudEnabled);
        config.put("encryptionEnabled", false);
        return config;
    }

    public Map<String, Object> saveCloudConfig(Map<String, Object> config) {
        if (config != null) {
            CLOUD_CONFIG.putAll(config);
        }
        return getCloudConfig();
    }

    /**
     * Bulut yedekleme yalnizca gercek bir uç nokta yapilandirildiginda calisir.
     * Yapilandirma yoksa basari donmek yerine acik hata verir (sahte basari yok).
     */
    public Map<String, Object> syncToCloud(String filename) {
        if (!cloudEnabled || cloudEndpoint == null || cloudEndpoint.isBlank()) {
            throw new BusinessException("Bulut yedekleme yapılandırılmamış. "
                    + "Gerçek bir sağlayıcı uç noktası tanımlanmadan buluta yükleme yapılamaz.");
        }
        Path file = backupPath.resolve(filename == null ? "" : filename).normalize();
        if (!file.startsWith(backupPath) || !Files.exists(file)) {
            throw new BusinessException("Yedek dosyası bulunamadı: " + filename);
        }
        try {
            byte[] icerik = Files.readAllBytes(file);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            restTemplate.put(cloudEndpoint, new HttpEntity<>(icerik, headers));
        } catch (Exception e) {
            log.error("Bulut senkronizasyonu başarısız", e);
            throw new BusinessException("Bulut senkronizasyonu başarısız: " + e.getMessage());
        }
        CLOUD_CONFIG.put("lastSyncTime", LocalDateTime.now().toString());
        return Map.of(
                "message", "Yedek bulut uç noktasına iletildi.",
                "filename", filename,
                "provider", CLOUD_CONFIG.get("provider"),
                "syncTime", CLOUD_CONFIG.get("lastSyncTime")
        );
    }
}