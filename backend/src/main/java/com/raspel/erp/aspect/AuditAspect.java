package com.raspel.erp.aspect;

import com.raspel.erp.service.sistem.AuditLogService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class AuditAspect {

    private final AuditLogService auditLogService;

    @Pointcut("execution(* com.raspel.erp.controller..*.olustur(..))")
    public void createPointcut() {}

    @Pointcut("execution(* com.raspel.erp.controller..*.guncelle(..))")
    public void updatePointcut() {}

    @Pointcut("execution(* com.raspel.erp.controller..*.sil(..))")
    public void deletePointcut() {}

    @Pointcut("execution(* com.raspel.erp.controller..*.durumGuncelle(..))")
    public void durumGuncellePointcut() {}

    @AfterReturning("createPointcut()")
    public void logCreate(JoinPoint jp) {
        log(jp, "OLUSTUR");
    }

    @AfterReturning("updatePointcut() || durumGuncellePointcut()")
    public void logUpdate(JoinPoint jp) {
        log(jp, "GUNCELLE");
    }

    @AfterReturning("deletePointcut()")
    public void logDelete(JoinPoint jp) {
        log(jp, "SIL");
    }

    @AfterThrowing(pointcut = "createPointcut() || updatePointcut() || deletePointcut() || durumGuncellePointcut()", throwing = "ex")
    public void logError(JoinPoint jp, Throwable ex) {
        log(jp, "HATA");
    }

    private void log(JoinPoint jp, String islem) {
        try {
            HttpServletRequest req = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
            Long kullaniciId = (Long) req.getAttribute("kullaniciId");
            Long sirketId = (Long) req.getAttribute("sirketId");
            String ip = req.getRemoteAddr();
            String entityAdi = jp.getTarget().getClass().getSimpleName().replace("Controller", "");
            Long entityId = null;
            Object[] args = jp.getArgs();
            if (args != null) {
                for (Object a : args) {
                    if (a instanceof Long) { entityId = (Long) a; break; }
                }
            }
            auditLogService.log(kullaniciId, sirketId, islem, entityAdi, entityId, "AOP: " + islem, ip);
        } catch (Exception e) {
            log.warn("Audit log kaydedilemedi: {}", e.getMessage());
        }
    }
}
