package com.raspel.erp.service;

import com.raspel.erp.entity.sistem.AuditLog;
import com.raspel.erp.repository.sistem.AuditLogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import com.raspel.erp.service.sistem.AuditLogService;
import com.raspel.erp.entity.sistem.Kullanici;

@ExtendWith(MockitoExtension.class)
class AuditLogServiceTest {

    @Mock private AuditLogRepository auditLogRepository;
    @InjectMocks private AuditLogService auditLogService;

    @Test
    void log_savesAuditLog() {
        auditLogService.log(1L, "CREATE", "Kullanici", 1L, "Yeni kullanici olusturuldu", "192.168.1.1");
        verify(auditLogRepository).save(any(AuditLog.class));
    }

    @Test
    void tumunuGetir_returnsAll() {
        AuditLog log1 = AuditLog.builder().id(1L).islem("CREATE").build();
        AuditLog log2 = AuditLog.builder().id(2L).islem("UPDATE").build();
        when(auditLogRepository.findAllByOrderByTarihDesc(Pageable.unpaged())).thenReturn(new PageImpl<>(List.of(log1, log2)));
        var result = auditLogService.tumunuGetir(Pageable.unpaged());
        assertEquals(2, result.getContent().size());
    }
}