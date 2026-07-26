package com.raspel.erp.service;

import com.raspel.erp.entity.AuditLog;
import com.raspel.erp.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public void log(Long kullaniciId, String islem, String entityAdi, Long entityId, String aciklama, String ipAdresi) {
        auditLogRepository.save(AuditLog.builder()
                .kullaniciId(kullaniciId).islem(islem).entityAdi(entityAdi)
                .entityId(entityId).aciklama(aciklama).ipAdresi(ipAdresi).build());
    }

    public Page<AuditLog> tumunuGetir(Pageable pageable) {
        return auditLogRepository.findAllByOrderByTarihDesc(pageable);
    }
}
