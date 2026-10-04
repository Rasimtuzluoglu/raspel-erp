package com.raspel.erp.service;

import com.raspel.erp.entity.sistem.AuditLog;
import com.raspel.erp.repository.sistem.AuditLogRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import com.raspel.erp.service.sistem.AuditLogService;

@ExtendWith(MockitoExtension.class)
class AuditLogServiceTest {

    @Mock private AuditLogRepository auditLogRepository;
    @InjectMocks private AuditLogService auditLogService;

    @AfterEach
    void temizle() {
        RequestContextHolder.resetRequestAttributes();
    }

    private void istekBaglami(Long kullaniciId, Long sirketId) {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setAttribute("kullaniciId", kullaniciId);
        req.setAttribute("sirketId", sirketId);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(req));
    }

    @Test
    void log_savesAuditLog() {
        auditLogService.log(1L, 1L, "CREATE", "Kullanici", 1L, "Yeni kullanici olusturuldu", "192.168.1.1");
        verify(auditLogRepository).save(any(AuditLog.class));
    }

    // ---------- C9: finansal silme denetimi is transaction'indan bagimsiz ----------

    /**
     * C9 kritik: `finansalSilmeLog` `REQUIRES_NEW` olmazsa çağıran transaction
     * rollback olduğunda (ör. finansal kayıt silindikten sonraki bir hata)
     * denetim kaydı da geri alınıyordu; yani "kayıt silindi" kanıtı kayboluyordu.
     */
    @Test
    void finansalSilmeLog_ayriTransactionKullanir() throws Exception {
        Method m = AuditLogService.class.getMethod(
                "finansalSilmeLog", String.class, Long.class, String.class);

        var propagation = m.getAnnotation(org.springframework.transaction.annotation.Transactional.class);

        assertNotNull(propagation, "finansalSilmeLog @Transactional olmali");
        assertEquals(org.springframework.transaction.annotation.Propagation.REQUIRES_NEW,
                propagation.propagation(),
                "Denetim kaydi is transaction'indan bagimsiz yazilmalidir");
    }

    /** C9: request baglami olan çağrı aktör bilgisiyle kaydedilir. */
    @Test
    void finansalSilmeLog_requestBaglamiOlanAktoruYazar() {
        istekBaglami(7L, 4L);

        auditLogService.finansalSilmeLog("Fatura", 55L, "Test detay");

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        AuditLog kayit = captor.getValue();
        assertEquals(7L, kayit.getKullaniciId());
        assertEquals(4L, kayit.getSirketId());
        assertEquals("SIL", kayit.getIslem());
        assertEquals("Fatura", kayit.getEntityAdi());
        assertEquals(55L, kayit.getEntityId());
        // finansalSilmeLog 7 argümanlı overload'ı kullanır; detay "aciklama" alanına yazılır.
        assertEquals("Test detay", kayit.getAciklama());
    }

    /** C9: scheduler/dahili çağrıda request yok; kayıt yine de yazılır. */
    @Test
    void finansalSilmeLog_requestBaglamiOlmadanKayitYazar() {
        auditLogService.finansalSilmeLog("Kasa", 3L, "Yedekleme temizligi");

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        AuditLog kayit = captor.getValue();
        assertNull(kayit.getKullaniciId(), "Request yoksa aktör null olmali");
        assertEquals("SIL", kayit.getIslem());
        assertEquals("Kasa", kayit.getEntityAdi());
    }

    /** C9: yazma hatasinda en azindan hata firlatilir, sessizce yutulmaz. */
    @Test
    void finansalSilmeLog_yazmaHatasindaYutmaz() {
        istekBaglami(7L, 4L);
        when(auditLogRepository.save(any(AuditLog.class)))
                .thenThrow(new RuntimeException("db hatasi"));

        assertDoesNotThrow(() -> auditLogService.finansalSilmeLog("Fatura", 55L, "detay"));
    }

    /** C9: tenant baglami olmadan denetim kaydi SORGULANMAZ (fail-closed). */
    @Test
    void filtreliGetir_tenantBaglamiYoksaBosDoner() {
        var sonuc = auditLogService.filtreliGetir(null, null, null, null, null, null,
                org.springframework.data.domain.PageRequest.of(0, 20));

        assertTrue(sonuc.isEmpty());
        verify(auditLogRepository, never()).filtreliGetir(any(), any(), any(), any(), any(), any(), any());
    }
}