package com.raspel.erp.service.sistem;

import com.raspel.erp.exception.BusinessException;
import com.raspel.erp.util.AesGcmUtil;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

/**
 * Veritabanı yedekleme servisi.
 *
 * <p>Kanonik dosya adı formatı:
 * <code>raspelerp_{TYPE}_{yyyyMMdd}_{HHmmss}.sql.gz</code> (Örn: raspelerp_DAILY_20260915_030000.sql.gz)
 * — TYPE: DAILY | WEEKLY | MONTHLY | YEARLY. Bu format hem bu serviste hem
 * scripts/backup.ps1 ve scripts/restore.ps1'de tutarlıdır.
 *
 * <p>Bulut (MinIO) kopyaları otomatik senkronizasyon açıksa alınır; şifreleme açıksa
 * kopya <code>.enc</code> sonekiyle AES-256-GCM olarak yazılır (yerel yedek düz kalır;
 * pratik geri yükleme için gerekir).
 */
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

    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private static final String KANONIK_PREFIKS = "raspelerp_";

    private static final String CALISTIRMA_ADVISORY_LOCK = "SELECT pg_try_advisory_lock(72495631)";

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

    @Value("${app.backup.retention-days:30}")
    private int retentionDays;

    @Value("${app.backup.auto-cron:0 0 3 * * ?}")
    private String autoCron;

    @Value("${app.backup.encryption-key:}")
    private String encryptionKey;

    private Path backupPath;

    private final DataSource dataSource;
    private final DosyaDepolamaService dosyaDepolama;
    private final JdbcTemplate jdbcTemplate;

    private static final Map<String, Object> CONFIG_VARSAYILAN = Map.of(
            "provider", "MINIO",
            "bucketName", "",
            "region", "",
            "autoSync", false,
            "encryptionEnabled", false,
            "lastSyncTime", ""
    );

    public BackupService(DataSource dataSource, DosyaDepolamaService dosyaDepolama, JdbcTemplate jdbcTemplate) {
        this.dataSource = dataSource;
        this.dosyaDepolama = dosyaDepolama;
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void init() {
        backupPath = Path.of(backupDir);
        try {
            Files.createDirectories(backupPath);
        } catch (java.io.IOException e) {
            log.error("Backup directory could not be created: {}", backupDir, e);
        }
        // Eski zamanlardan kalan schedule.json kalıntısını temizle (artık kullanılmıyor).
        try {
            Files.deleteIfExists(backupPath.resolve("schedule.json"));
        } catch (java.io.IOException ignored) {
        }
        log.info("BackupService initialized. Dir: {}, retentionDays: {}, autoCron: {}", backupDir, retentionDays, autoCron);
    }

    // ------------------------------------------------------------------
    // Dosya adlandırma / tanıma
    // ------------------------------------------------------------------

    private String yeniDosyaAdi(String type) {
        return KANONIK_PREFIKS + type + "_" + LocalDateTime.now().format(TIMESTAMP_FORMAT) + ".sql.gz";
    }

    private boolean isBackupDosyasi(String name) {
        return name.startsWith(KANONIK_PREFIKS)
                || (name.startsWith("raspelerp-") && !name.equals("schedule.json"));
    }

    /**
     * Hem kanonik (raspelerp_TYPE_) hem eski (raspelerp-TYPE-) formatları çözer.
     */
    private String parseType(String filename) {
        for (String t : TYPE_ORDER) {
            if (filename.contains("_" + t + "_") || filename.contains("-" + t + "-")) return t;
        }
        return "DAILY";
    }

    private boolean gecerliIsim(String filename) {
        return filename != null
                && !filename.contains("/")
                && !filename.contains("\\")
                && !filename.startsWith(".")
                && !filename.equals("..");
    }

    // ------------------------------------------------------------------
    // Yedek alma
    // ------------------------------------------------------------------

    public String manualBackup(String type) {
        if (type == null || !RETENTION_MAP.containsKey(type)) type = "DAILY";
        String filename = yeniDosyaAdi(type);
        Path outputFile = backupPath.resolve(filename);

        try {
            ProcessBuilder pb = new ProcessBuilder(
                    "pg_dump",
                    "-h", dbHost,
                    "-p", dbPort,
                    "-U", dbUser(),
                    "-d", dbName,
                    "--no-owner",
                    "--no-acl",
                    "--clean",
                    "--if-exists"
            );
            pb.environment().put("PGPASSWORD", dbPassword());

            Process dump = pb.start();
            ProcessBuilder gzip = new ProcessBuilder("gzip", "-f");
            java.io.File gzipOut = outputFile.toFile();
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
            if (otomatikBulutSenkronAktif()) {
                bulutaAktar(outputFile, filename);
            }
            return filename;
        } catch (Exception e) {
            log.error("Backup failed", e);
            throw new RuntimeException("Backup failed: " + e.getMessage());
        }
    }

    private String dbUser() {
        String dbUser = System.getenv("DB_USERNAME");
        return (dbUser == null || dbUser.isBlank()) ? "postgres" : dbUser;
    }

    private String dbPassword() {
        String dbPass = dbPasswordProperty;
        if (dbPass == null || dbPass.isBlank()) {
            dbPass = System.getenv("DB_PASSWORD");
        }
        return dbPass == null ? "" : dbPass;
    }

    // ------------------------------------------------------------------
    // Bulut (MinIO) entegrasyonu
    // ------------------------------------------------------------------

    private boolean bulutAktif() {
        return cloudEnabled || (dosyaDepolama != null && dosyaDepolama.isMinioAktif());
    }

    private Map<String, Object> mevcutConfig() {
        Map<String, Object> kalici = kaliciConfigOku();
        if (kalici == null) return new HashMap<>(CONFIG_VARSAYILAN);
        return kalici;
    }

    private boolean sifrelemeAktif() {
        Object v = mevcutConfig().get("encryptionEnabled");
        return Boolean.TRUE.equals(v);
    }

    private boolean otomatikBulutSenkronAktif() {
        if (!bulutAktif()) return false;
        Object v = mevcutConfig().get("autoSync");
        return Boolean.TRUE.equals(v);
    }

    private String sifrelemeAnahtari() {
        if (encryptionKey != null && !encryptionKey.isBlank()) return encryptionKey;
        String env = System.getenv("AI_ENCRYPTION_KEY");
        if (env != null && !env.isBlank()) return env;
        return null;
    }

    /**
     * Yerel yedek dosyasının şifreli (varsa) kopyasını MinIO "backups/" klasörüne yazar.
     */
    private void bulutaAktar(Path localFile, String filename) {
        if (!bulutAktif()) return;
        try {
            byte[] icerik = Files.readAllBytes(localFile);
            String nesneAdi = filename;
            String nesne = "düz";
            if (sifrelemeAktif()) {
                String key = sifrelemeAnahtari();
                if (key == null) {
                    log.warn("Şifreleme açık ama anahtar yok (APP_BACKUP_ENCRYPTION_KEY / AI_ENCRYPTION_KEY), düz kopya alınmadı: {}", filename);
                    throw new BusinessException("Şifreleme anahtarı tanımlı değil. AES kopyası oluşturulamadı.");
                }
                nesneAdi = filename + ".enc";
                icerik = AesGcmUtil.encrypt(icerik, key);
                nesne = "şifreli";
            }
            dosyaDepolama.kaydetBytes("backups", nesneAdi, icerik, "application/octet-stream");
            log.info("Yedek buluta aktarıldı ({}): {}", nesne, nesneAdi);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Yedek buluta aktarılamadı ({}): {}", filename, e.getMessage());
        }
    }

    /**
     * MinIO'dan ham/şifreli içeriği okur; .enc ise çözer.
     */
    private byte[] buluttanVeriOku(String filename) {
        if (!bulutAktif() || !gecerliIsim(filename)) return null;
        try {
            DosyaDepolamaService.DepolananDosya dosya = dosyaDepolama.getir("backups", filename);
            if (dosya == null) return null;
            byte[] veri = dosya.icerik();
            if (filename.endsWith(".enc")) {
                String key = sifrelemeAnahtari();
                if (key == null) {
                    log.warn("Şifreli bulut yedeği çözülemedi: anahtar yok ({})", filename);
                    return null;
                }
                return AesGcmUtil.decrypt(veri, key);
            }
            return veri;
        } catch (Exception e) {
            log.warn("Bulut yedeği okunamadı ({}): {}", filename, e.getMessage());
            return null;
        }
    }

    /**
     * Otomatik bulut senkronizasyonu: günlük yedekten sonra en güncel yedeği buluta aktarır.
     * autoSync kapalıysa atlanır.
     */
    @Scheduled(cron = "${app.backup.cloud-auto-cron:0 30 3 * * ?}")
    public void autoCloudSync() {
        if (!otomatikBulutSenkronAktif()) return;
        log.info("Otomatik bulut senkronizasyonu başladı");
        try {
            syncToCloud(null);
        } catch (Exception e) {
            log.error("Otomatik bulut senkronizasyonu başarısız", e);
        }
    }

    // ------------------------------------------------------------------
    // Kalıcı bulut yapılandırması (DB: sistem.backup_ayar)
    // ------------------------------------------------------------------

    private Map<String, Object> kaliciConfigOku() {
        if (jdbcTemplate == null) return null;
        try {
            return jdbcTemplate.queryForObject(
                    "SELECT provider, bucket, region, auto_sync, encryption_enabled, last_sync_time FROM sistem.backup_ayar WHERE id = 1",
                    (rs, rowNum) -> {
                        Map<String, Object> m = new HashMap<>();
                        m.put("provider", rs.getString("provider"));
                        m.put("bucketName", rs.getString("bucket"));
                        m.put("region", rs.getString("region"));
                        m.put("autoSync", rs.getBoolean("auto_sync"));
                        m.put("encryptionEnabled", rs.getBoolean("encryption_enabled"));
                        var ts = rs.getTimestamp("last_sync_time");
                        m.put("lastSyncTime", ts == null ? "" : ts.toLocalDateTime().toString());
                        return m;
                    });
        } catch (Exception e) {
            log.warn("Kalıcı bulut ayarı okunamadı (tablo yoksa normal): {}", e.getMessage());
            return null;
        }
    }

    private void kaliciConfigYaz(Map<String, Object> cfg) {
        if (jdbcTemplate == null) return;
        try {
            jdbcTemplate.update(
                    "INSERT INTO sistem.backup_ayar (id, provider, bucket, region, auto_sync, encryption_enabled, last_sync_time, guncelleme_tarihi) "
                            + "VALUES (1, ?, ?, ?, ?, ?, NULL, now()) "
                            + "ON CONFLICT (id) DO UPDATE SET provider = EXCLUDED.provider, bucket = EXCLUDED.bucket, "
                            + "region = EXCLUDED.region, auto_sync = EXCLUDED.auto_sync, encryption_enabled = EXCLUDED.encryption_enabled, "
                            + "guncelleme_tarihi = now()",
                    str(cfg, "provider"),
                    str(cfg, "bucketName"),
                    str(cfg, "region"),
                    bool(cfg, "autoSync"),
                    bool(cfg, "encryptionEnabled"));
        } catch (Exception e) {
            log.warn("Kalıcı bulut ayarı yazılamadı: {}", e.getMessage());
        }
    }

    private void sonSenkronGuncelle() {
        if (jdbcTemplate == null) return;
        try {
            jdbcTemplate.update("UPDATE sistem.backup_ayar SET last_sync_time = now() WHERE id = 1");
        } catch (Exception ignored) {
        }
    }

    private static String str(Map<String, Object> cfg, String key) {
        Object v = cfg.get(key);
        return v == null ? "" : String.valueOf(v);
    }

    private static boolean bool(Map<String, Object> cfg, String key) {
        return Boolean.TRUE.equals(cfg.get(key));
    }

    // ------------------------------------------------------------------
    // Liste / İndir / Sil / Geri yükle
    // ------------------------------------------------------------------

    public List<Map<String, Object>> listBackups() {
        List<Map<String, Object>> list = new ArrayList<>();
        try {
            List<Path> files = Files.list(backupPath)
                    .filter(f -> isBackupDosyasi(f.getFileName().toString()))
                    .sorted(Comparator.reverseOrder())
                    .collect(Collectors.toList());

            for (Path f : files) {
                Map<String, Object> item = new LinkedHashMap<>();
                String name = f.getFileName().toString();
                item.put("filename", name);
                item.put("size", Files.size(f));
                item.put("lastModified", Files.getLastModifiedTime(f).toMillis());
                item.put("type", parseType(name));
                item.put("location", "YEREL");
                list.add(item);
            }
        } catch (java.io.IOException e) {
            log.error("Failed to list local backups", e);
        }

        // MinIO'da olup yerelde olmayan kopyaları ekle.
        if (bulutAktif()) {
            try {
                for (DosyaDepolamaService.NesneBilgi nesne : dosyaDepolama.listele("backups")) {
                    String ad = nesne.ad();
                    if (!isBackupDosyasi(ad)) continue;
                    boolean yereldeVar = Files.exists(backupPath.resolve(ad));
                    if (yereldeVar) continue;
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("filename", ad);
                    item.put("size", nesne.boyut());
                    item.put("lastModified", nesne.sonDegistirme());
                    item.put("type", parseType(ad));
                    item.put("location", "BULUT");
                    if (ad.endsWith(".enc")) {
                        item.put("encrypted", true);
                    }
                    list.add(item);
                }
            } catch (Exception e) {
                log.warn("Bulut yedekleri listelenemedi: {}", e.getMessage());
            }
        }
        return list;
    }

    public byte[] downloadBackup(String filename) {
        if (!gecerliIsim(filename)) {
            throw new RuntimeException("Geçersiz dosya adı: " + filename);
        }
        Path file = backupPath.resolve(filename);
        if (Files.exists(file) && file.normalize().startsWith(backupPath)) {
            try {
                return Files.readAllBytes(file);
            } catch (java.io.IOException e) {
                throw new RuntimeException("Failed to read backup file: " + filename, e);
            }
        }
        byte[] bulut = buluttanVeriOku(filename);
        if (bulut != null) return bulut;
        throw new RuntimeException("Backup file not found: " + filename);
    }

    public void deleteBackup(String filename) {
        if (!gecerliIsim(filename)) {
            throw new RuntimeException("Geçersiz dosya adı: " + filename);
        }
        Path file = backupPath.resolve(filename);
        boolean yerelVardi = Files.exists(file) && file.normalize().startsWith(backupPath);
        if (yerelVardi) {
            try {
                Files.delete(file);
                log.info("Backup deleted: {}", filename);
            } catch (java.io.IOException e) {
                throw new RuntimeException("Failed to delete backup: " + filename, e);
            }
        }
        // MinIO cascade: hem düz hem .enc kopyaları sil.
        if (bulutAktif()) {
            dosyaDepolama.sil("backups", filename);
            dosyaDepolama.sil("backups", filename + ".enc");
        }
        if (!yerelVardi && !bulutAktif()) {
            throw new RuntimeException("Backup file not found: " + filename);
        }
    }

    /**
     * Bir yedek dosyasını geri yükler (gunzip | psql). Dosya .sql.gz formatında olmalıdır.
     * Yerelde yoksa MinIO'dan indirir (şifreliyse çözer).
     */
    public String restoreBackup(String filename) {
        if (!gecerliIsim(filename)) {
            throw new RuntimeException("Geçersiz dosya adı: " + filename);
        }
        Path file = backupPath.resolve(filename).normalize();
        boolean yerelVar = file.startsWith(backupPath) && Files.exists(file);
        Path restoreKaynak = file;
        Path gecici = null;

        if (!yerelVar) {
            if (!bulutAktif()) {
                throw new RuntimeException("Backup file not found: " + filename);
            }
            byte[] veri = buluttanVeriOku(filename);
            if (veri == null) {
                throw new RuntimeException("Backup file not found: " + filename);
            }
            try {
                gecici = Files.createTempFile("raspel-restore-", ".sql.gz");
                Files.write(gecici, veri);
                restoreKaynak = gecici;
            } catch (java.io.IOException e) {
                throw new RuntimeException("Geçici yedek dosyası oluşturulamadı: " + filename, e);
            }
        }

        try {
            ProcessBuilder psql = new ProcessBuilder(
                    "psql",
                    "-h", dbHost,
                    "-p", dbPort,
                    "-U", dbUser(),
                    "-d", dbName,
                    "--set", "ON_ERROR_STOP=1"
            );
            psql.environment().put("PGPASSWORD", dbPassword());
            Process psqlProc = psql.start();

            ProcessBuilder gunzip = new ProcessBuilder("gunzip", "-c", restoreKaynak.toString());
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
        } finally {
            if (gecici != null) {
                try {
                    Files.deleteIfExists(gecici);
                } catch (java.io.IOException ignored) {
                }
            }
        }
    }

    // ------------------------------------------------------------------
    // Zamanlama & temizlik
    // ------------------------------------------------------------------

    /**
     * Çoklu instance ortamında aynı anda yalnızca bir örneğin yedek almasını
     * sağlar (PostgreSQL advisory lock). Kilit alınamazsa görev atlanır.
     */
    private boolean yedekKilidiniAl() {
        try (Connection conn = dataSource.getConnection();
             Statement st = conn.createStatement()) {
            try (ResultSet rs = st.executeQuery(CALISTIRMA_ADVISORY_LOCK)) {
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

    @Scheduled(cron = "${app.backup.auto-cron:0 0 3 * * ?}")
    public void dailyAutoBackup() {
        if (!yedekKilidiniAl()) return;
        log.info("Daily auto backup started (cron: {})", autoCron);
        try {
            manualBackup("DAILY");
            cleanOldBackups("DAILY", retentionFor("DAILY"));
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
            cleanOldBackups("WEEKLY", retentionFor("WEEKLY"));
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
            cleanOldBackups("MONTHLY", retentionFor("MONTHLY"));
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

    /**
     * DAILY için env'den gelen app.backup.retention-days, diğerleri statik tablo.
     */
    private int retentionFor(String type) {
        if ("DAILY".equals(type)) return retentionDays;
        return RETENTION_MAP.getOrDefault(type, -1);
    }

    private void cleanOldBackups(String type, int retentionDays) {
        if (retentionDays < 0) return;
        long cutoff = System.currentTimeMillis() - retentionDays * 86400000L;
        try {
            List<Path> oldFiles = Files.list(backupPath)
                    .filter(f -> f.getFileName().toString().startsWith(KANONIK_PREFIKS + type + "_"))
                    .filter(f -> {
                        try { return Files.getLastModifiedTime(f).toMillis() < cutoff; }
                        catch (java.io.IOException e) { return false; }
                    })
                    .collect(Collectors.toList());

            for (Path f : oldFiles) {
                String ad = f.getFileName().toString();
                Files.delete(f);
                // Bulut kopyalarını da (düz + .enc) aynı retention'a tabi tut.
                if (bulutAktif()) {
                    dosyaDepolama.sil("backups", ad);
                    dosyaDepolama.sil("backups", ad + ".enc");
                }
                log.info("Old {} backup deleted: {}", type, ad);
            }
            if (!oldFiles.isEmpty()) {
                log.info("Cleaned {} old {} backup(s)", oldFiles.size(), type);
            }
        } catch (java.io.IOException e) {
            log.error("Failed to clean old {} backups", type, e);
        }
    }

    public void cleanAllOldBackups() {
        for (String t : RETENTION_MAP.keySet()) {
            cleanOldBackups(t, retentionFor(t));
        }
    }

    // ------------------------------------------------------------------
    // Zamanlama / durum bilgisi
    // ------------------------------------------------------------------

    public Map<String, Object> getSchedule() {
        Map<String, Object> schedule = new LinkedHashMap<>();
        schedule.put("retention", Map.of(
                "DAILY", retentionFor("DAILY"),
                "WEEKLY", retentionFor("WEEKLY"),
                "MONTHLY", retentionFor("MONTHLY"),
                "YEARLY", retentionFor("YEARLY")
        ));
        schedule.put("autoCron", autoCron);
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
        try {
            InputStream kaynak;
            if (Files.exists(file) && file.normalize().startsWith(backupPath)) {
                kaynak = Files.newInputStream(file);
            } else {
                byte[] veri = buluttanVeriOku(filename);
                if (veri == null) return false;
                Path gecici = Files.createTempFile("raspel-dogrula-", ".gz");
                Files.write(gecici, veri);
                kaynak = Files.newInputStream(gecici);
            }
            try (kaynak; GZIPInputStream gz = new GZIPInputStream(kaynak)) {
                byte[] buf = new byte[8192];
                long total = 0;
                int n;
                while ((n = gz.read(buf)) != -1) total += n;
                return total > 0;
            }
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Günlük yedek doğrulama health-check'i. Son yedek bayat ya da bozuksa uyarı loglar.
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

    private String getLastBackupTime() {
        try {
            Optional<Path> latest = Files.list(backupPath)
                    .filter(f -> isBackupDosyasi(f.getFileName().toString()))
                    .max(Comparator.comparing(f -> {
                        try { return Files.getLastModifiedTime(f); }
                        catch (java.io.IOException e) { return null; }
                    }));
            if (latest.isPresent()) {
                return Files.getLastModifiedTime(latest.get()).toString();
            }
        } catch (java.io.IOException ignored) {}
        return null;
    }

    private long getTotalBackupSize() {
        try {
            return Files.list(backupPath)
                    .filter(f -> isBackupDosyasi(f.getFileName().toString()))
                    .mapToLong(f -> {
                        try { return Files.size(f); }
                        catch (java.io.IOException e) { return 0; }
                    })
                    .sum();
        } catch (java.io.IOException e) {
            return 0;
        }
    }

    private Map<String, Long> getTypeCounts() {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (String t : TYPE_ORDER) counts.put(t, 0L);
        try {
            Files.list(backupPath)
                    .filter(f -> isBackupDosyasi(f.getFileName().toString()))
                    .forEach(f -> {
                        String t = parseType(f.getFileName().toString());
                        counts.merge(t, 1L, Long::sum);
                    });
        } catch (java.io.IOException ignored) {}
        return counts;
    }

    // ------------------------------------------------------------------
    // Bulut yapılandırma API
    // ------------------------------------------------------------------

    public Map<String, Object> getCloudConfig() {
        Map<String, Object> cfg = mevcutConfig();
        cfg.put("status", bulutAktif() ? "AKTIF" : "DEVRE_DISI");
        cfg.put("enabled", bulutAktif());
        cfg.put("provider", str(cfg, "provider"));
        cfg.put("bucketName", str(cfg, "bucketName"));
        cfg.put("region", str(cfg, "region"));
        cfg.put("encryptionAlgorithm", "AES-256-GCM");
        if (cfg.get("lastSyncTime") == null) cfg.put("lastSyncTime", "");
        return cfg;
    }

    public Map<String, Object> saveCloudConfig(Map<String, Object> config) {
        Map<String, Object> merged = new HashMap<>(CONFIG_VARSAYILAN);
        Map<String, Object> kalici = kaliciConfigOku();
        if (kalici != null) merged.putAll(kalici);
        if (config != null) merged.putAll(config);
        // Sadece desteklenen anahtarları yaz.
        Map<String, Object> duzgun = new HashMap<>();
        duzgun.put("provider", str(merged, "provider"));
        duzgun.put("bucketName", str(merged, "bucketName"));
        duzgun.put("region", str(merged, "region"));
        duzgun.put("autoSync", bool(merged, "autoSync"));
        duzgun.put("encryptionEnabled", bool(merged, "encryptionEnabled"));
        kaliciConfigYaz(duzgun);
        return getCloudConfig();
    }

    /**
     * Bulut (MinIO) senkronizasyonu. MinIO aktif değilse ya da dosya yoksa açık hata
     * döner (sahte başarı yok). Şifreleme açıksa kopya .enc olarak yazılır.
     */
    public Map<String, Object> syncToCloud(String filename) {
        if (!bulutAktif()) {
            throw new BusinessException("Bulut depolama yapılandırılmamış. "
                    + "MinIO aktif değil (APP_STORAGE_TYPE=minio) — buluta senkronize edilemez.");
        }
        if (filename == null || filename.isBlank()) {
            List<Map<String, Object>> backups = listBackups();
            if (backups.isEmpty()) {
                throw new BusinessException("Senkronize edilecek yedek bulunamadı. Önce yedek alın.");
            }
            filename = (String) backups.get(0).get("filename");
        }
        if (!gecerliIsim(filename)) {
            throw new BusinessException("Geçersiz dosya adı: " + filename);
        }
        Path file = backupPath.resolve(filename).normalize();
        if (!file.startsWith(backupPath) || !Files.exists(file)) {
            throw new BusinessException("Yedek dosyası bulunamadı: " + filename);
        }
        try {
            bulutaAktar(file, filename);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Bulut senkronizasyonu başarısız", e);
            throw new BusinessException("Bulut senkronizasyonu başarısız: " + e.getMessage());
        }
        sonSenkronGuncelle();
        Map<String, Object> cfg = mevcutConfig();
        return Map.of(
                "message", "Yedek bulut (MinIO) deposuna iletildi.",
                "filename", filename,
                "provider", str(cfg, "provider"),
                "encrypted", sifrelemeAktif(),
                "syncTime", LocalDateTime.now().toString()
        );
    }
}