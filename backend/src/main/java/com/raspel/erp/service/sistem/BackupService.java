package com.raspel.erp.service.sistem;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
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

    private Path backupPath;
    private Path schedulePath;

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
                    "--no-acl"
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

    @Scheduled(cron = "0 0 3 * * ?")
    public void dailyAutoBackup() {
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
}