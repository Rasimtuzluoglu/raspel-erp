package com.raspel.erp.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.raspel.erp.service.sistem.AuditLogService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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

import java.util.ArrayList;
import java.util.List;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class AuditAspect {

    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;

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
            String detay = detayOlustur(args);
            auditLogService.log(kullaniciId, sirketId, islem, entityAdi, entityId, "AOP: " + islem, ip, detay);
        } catch (Exception e) {
            log.warn("Audit log kaydedilemedi: {}", e.getMessage());
        }
    }

    /**
     * Metot argümanlarındaki DTO'ları JSON olarak yakalar. Bu, değişikliğin
     * "sonra" (gönderilen) değerlerini denetim izine yazar. ID, HttpServletRequest/Response
     * gibi teknik parametreler hariç tutulur.
     */
    private String detayOlustur(Object[] args) {
        if (args == null) return null;
        List<Object> anlamli = new ArrayList<>();
        for (Object a : args) {
            if (a == null) continue;
            if (a instanceof Long || a instanceof Integer) continue;
            if (a instanceof HttpServletRequest || a instanceof HttpServletResponse) continue;
            anlamli.add(a);
        }
        if (anlamli.isEmpty()) return null;
        try {
            Object hedef = anlamli.size() == 1 ? anlamli.get(0) : anlamli;
            return objectMapper.writeValueAsString(hedef);
        } catch (Exception e) {
            return null;
        }
    }
}
