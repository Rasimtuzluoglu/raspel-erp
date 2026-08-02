package com.raspel.erp.controller;

import com.raspel.erp.entity.AuditLog;
import com.raspel.erp.service.AuditLogService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.annotation.DirtiesContext;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuditLogController.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@AutoConfigureMockMvc(addFilters = false)
@Import(TestSecurityMocks.class)
class AuditLogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    
    @MockBean
    private AuditLogService auditLogService;

    @Test
    void shouldGetAll() throws Exception {
        var list = List.of(
                AuditLog.builder().id(1L).islem("GIRIS").entityAdi("Kullanici").entityId(1L).tarih(LocalDateTime.now()).build()
        );
        when(auditLogService.filtreliGetir(isNull(), isNull(), isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(list));

        mockMvc.perform(get("/api/audit-log"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].islem").value("GIRIS"))
                .andExpect(jsonPath("$.content[0].entityAdi").value("Kullanici"));
    }

    @Test
    void shouldFilterByIslem() throws Exception {
        var list = List.of(
                AuditLog.builder().id(1L).islem("SIL").entityAdi("Cari").tarih(LocalDateTime.now()).build()
        );
        when(auditLogService.filtreliGetir(isNull(), eq("SIL"), isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(list));

        mockMvc.perform(get("/api/audit-log").param("islem", "SIL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].islem").value("SIL"));
    }

    @Test
    void shouldGetIslemTipleri() throws Exception {
        when(auditLogService.islemTipleri()).thenReturn(List.of("GIRIS", "SIL"));

        mockMvc.perform(get("/api/audit-log/islem-tipleri"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value("GIRIS"));
    }
}








