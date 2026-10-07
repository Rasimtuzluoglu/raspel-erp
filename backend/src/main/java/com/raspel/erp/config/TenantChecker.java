package com.raspel.erp.config;

import com.raspel.erp.exception.ResourceNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
public class TenantChecker {

    /**
     * O an bir HTTP istek bağlamı olup olmadığını döndürür. Zamanlanmış işler
     * (ShedLock) ve unit testler request bağlamı olmadan çalışır; bu ayrım
     * fail-closed davranışın iç çağrıları kırmaması için gereklidir.
     */
    public boolean hasRequestContext() {
        try {
            RequestContextHolder.currentRequestAttributes();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public Long getCurrentSirketId() {
        try {
            HttpServletRequest req = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
            return (Long) req.getAttribute("sirketId");
        } catch (Exception e) {
            return null;
        }
    }

    public Long getCurrentKullaniciId() {
        try {
            HttpServletRequest req = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
            return (Long) req.getAttribute("kullaniciId");
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Cache anahtarlarina tenant bilgisi ekler. Boylece ayni ID'ye sahip kayitlar
     * farkli sirketler icin ayri cache entry'lerinde tutulur ve cache HIT'inde
     * sirket izolasyonu atlanamaz. Request context yoksa (dahili cagrilar) ID
     * oldugu gibi kullanilir.
     */
    public static String tenantKey(Object id) {
        try {
            HttpServletRequest req = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
            Object sirketId = req.getAttribute("sirketId");
            return id + ":" + (sirketId != null ? sirketId : "");
        } catch (Exception e) {
            return String.valueOf(id);
        }
    }

    public void check(Long entitySirketId, String entityName) {
        // Request baglami yoksa (zamanlanmis is / unit test) tenant dogrulamasi
        // yapilamaz; bu durumda ic cagri oldugu icin izin verilir.
        if (!hasRequestContext()) {
            return;
        }
        Long currentSirketId = getCurrentSirketId();
        // Fail-closed: HTTP isteginde tenant baglami YOKSA (or. JWT'de sirketId
        // claim'i yoksa) tenant kontrolu sessizce atlanmaz; erisim reddedilir.
        if (currentSirketId == null || entitySirketId == null || !currentSirketId.equals(entitySirketId)) {
            throw new ResourceNotFoundException(entityName + " bu sirkete ait degil");
        }
    }

    /**
     * Yazma islemlerinde DTO'dan gelen sirketId'nin JWT'deki sirketId ile
     * eslesmesini zorunlu kilar. Request context yoksa (test ortami / dahili
     * cagrilar) dogrulama yapilmaz.
     */
    public void checkSirketId(Long dtoSirketId, String entityName) {
        Long currentSirketId = getCurrentSirketId();
        if (currentSirketId != null && dtoSirketId != null && !currentSirketId.equals(dtoSirketId)) {
            throw new ResourceNotFoundException(entityName + " bu sirkete ait degil");
        }
    }
}
