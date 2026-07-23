package com.raspel.erp.aspect;

import com.raspel.erp.service.AuditLogService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class AuditAspect {

    private final AuditLogService auditLogService;

    @AfterReturning("execution(* com.raspel.erp.controller.*.olustur(..))")
    public void logCreate(JoinPoint jp) {
        log(jp, "OLUSTUR");
    }

    @AfterReturning("execution(* com.raspel.erp.controller.*.sil(..))")
    public void logDelete(JoinPoint jp) {
        log(jp, "SIL");
    }

    @AfterReturning("execution(* com.raspel.erp.controller.*.durumGuncelle(..))")
    public void logUpdate(JoinPoint jp) {
        log(jp, "DURUM_GUNCELLE");
    }

    private void log(JoinPoint jp, String islem) {
        try {
            HttpServletRequest req = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
            Long kullaniciId = req.getAttribute("kullaniciId") != null ? (Long) req.getAttribute("kullaniciId") : null;
            String ip = req.getRemoteAddr();
            String entityAdi = jp.getTarget().getClass().getSimpleName().replace("Controller", "");
            Long entityId = null;
            Object[] args = jp.getArgs();
            if (args != null) {
                for (Object a : args) {
                    if (a instanceof Long) { entityId = (Long) a; break; }
                }
            }
            auditLogService.log(kullaniciId, islem, entityAdi, entityId, "AOP: " + islem, ip);
        } catch (Exception e) {
            log.warn("Audit log kaydedilemedi: {}", e.getMessage());
        }
    }
}
