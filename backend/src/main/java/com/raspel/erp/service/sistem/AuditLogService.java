package com.raspel.erp.service.sistem;

import com.raspel.erp.entity.sistem.AuditLog;
import com.raspel.erp.repository.sistem.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public void log(Long kullaniciId, Long sirketId, String islem, String entityAdi, Long entityId, String aciklama, String ipAdresi) {
        auditLogRepository.save(AuditLog.builder()
                .kullaniciId(kullaniciId).sirketId(sirketId).islem(islem).entityAdi(entityAdi)
                .entityId(entityId).aciklama(aciklama).ipAdresi(ipAdresi).build());
    }

    public Page<AuditLog> tumunuGetir(Pageable pageable) {
        return auditLogRepository.findAllByOrderByTarihDesc(pageable);
    }

    public Page<AuditLog> filtreliGetir(Long sirketId, Long kullaniciId, String islem, String entityAdi,
                                         LocalDate baslangicTarih, LocalDate bitisTarih, Pageable pageable) {
        LocalDateTime baslangic = baslangicTarih != null ? baslangicTarih.atStartOfDay() : null;
        LocalDateTime bitis = bitisTarih != null ? bitisTarih.atTime(LocalTime.MAX) : null;
        return auditLogRepository.filtreliGetir(sirketId, kullaniciId, islem, entityAdi, baslangic, bitis, pageable);
    }

    public List<String> islemTipleri() {
        return auditLogRepository.findDistinctIslem();
    }

    public List<String> entityListesi() {
        return auditLogRepository.findDistinctEntityAdi();
    }
}
