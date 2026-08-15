package com.raspel.erp.controller.sistem;

import com.raspel.erp.controller.TestSecurityMocks;
import com.raspel.erp.service.sistem.BackupService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BackupController.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@AutoConfigureMockMvc(addFilters = false)
@Import(TestSecurityMocks.class)
class BackupControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private BackupService backupService;

    @Test
    void shouldManualBackup() throws Exception {
        when(backupService.manualBackup("DAILY")).thenReturn("backup-daily.sql");
        mockMvc.perform(post("/api/backups/manual").param("type", "DAILY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.filename").value("backup-daily.sql"));
    }

    @Test
    void shouldListBackups() throws Exception {
        when(backupService.listBackups()).thenReturn(List.of(Map.of("filename", "a.sql")));
        mockMvc.perform(get("/api/backups"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].filename").value("a.sql"));
    }

    @Test
    void shouldDownloadBackup() throws Exception {
        when(backupService.downloadBackup("a.sql")).thenReturn(new byte[] { 1, 2, 3 });
        mockMvc.perform(get("/api/backups/download/a.sql"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldDeleteBackup() throws Exception {
        doNothing().when(backupService).deleteBackup("a.sql");
        mockMvc.perform(delete("/api/backups/a.sql"))
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldGetSchedule() throws Exception {
        when(backupService.getSchedule()).thenReturn(Map.of("daily", true));
        mockMvc.perform(get("/api/backups/schedule"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.daily").value(true));
    }
}
