package com.raspel.erp.service;

import com.raspel.erp.entity.sistem.AuditLog;
import com.raspel.erp.repository.sistem.AuditLogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import com.raspel.erp.service.sistem.AuditLogService;

@ExtendWith(MockitoExtension.class)
class AuditLogServiceTest {

    @Mock private AuditLogRepository auditLogRepository;
    @InjectMocks private AuditLogService auditLogService;

    @Test
    void log_savesAuditLog() {
        auditLogService.log(1L, 1L, "CREATE", "Kullanici", 1L, "Yeni kullanici olusturuldu", "192.168.1.1");
        verify(auditLogRepository).save(any(AuditLog.class));
    }
}