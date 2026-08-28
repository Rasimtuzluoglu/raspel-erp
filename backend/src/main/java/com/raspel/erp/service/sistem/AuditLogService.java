package com.raspel.erp.service.sistem;

import com.raspel.erp.entity.sistem.AuditLog;
import com.raspel.erp.repository.sistem.AuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public void log(Long kullaniciId, Long sirketId, String islem, String entityAdi, Long entityId, String aciklama, String ipAdresi) {
        log(kullaniciId, sirketId, islem, entityAdi, entityId, aciklama, ipAdresi, null);
    }

    public void log(Long kullaniciId, Long sirketId, String islem, String entityAdi, Long entityId, String aciklama, String ipAdresi, String detay) {
        auditLogRepository.save(AuditLog.builder()
                .kullaniciId(kullaniciId).sirketId(sirketId).islem(islem).entityAdi(entityAdi)
                .entityId(entityId).aciklama(aciklama).ipAdresi(ipAdresi).detay(detay).build());
    }

    /**
     * Finansal kayıt silmelerinde zengin denetim izi bırakır: silinen tutar/tür/cari gibi
     * detaylar AOP kaydının yanı sıra aciklama alanına yazılır (Denetim ekranında görünür).
     */
    public void finansalSilmeLog(String entityAdi, Long entityId, String detay) {
        try {
            HttpServletRequest req = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
            Long kullaniciId = (Long) req.getAttribute("kullaniciId");
            Long sirketId = (Long) req.getAttribute("sirketId");
            log(kullaniciId, sirketId, "SIL", entityAdi, entityId, detay, req.getRemoteAddr());
        } catch (Exception ignored) {
            // Request context yoksa (test/dahili çağrı) denetim kaydı atlanır
        }
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
