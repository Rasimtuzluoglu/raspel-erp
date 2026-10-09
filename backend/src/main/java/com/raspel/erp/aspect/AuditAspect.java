package com.raspel.erp.aspect;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
import java.util.Set;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class AuditAspect {

    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;

    /** Denetim izine yazılmaması gereken hassas alanlar (küçük harf karşılaştırılır). */
    private static final Set<String> HASSAS_ALANLAR = Set.of(
            "password", "sifre", "secret", "apikey", "api_key",
            "token", "twostepsecret", "twofactorsecret", "two_factor_secret"
    );

    /**
     * Kritik mutasyon noktalari. Controller metot adlari buyuk harfle bitebildigi
     * icin (faturaOlustur, faturaSil, iptaliGeriAl, bankayaAktar...) hem eski hem
     * yeni adlar kapsanir. TEK advice kullanildigi icin bir metot birden fazla
     * desene uysa bile yalnizca bir kez denetim izine yazilir.
     */
    @Pointcut("execution(* com.raspel.erp.controller..*.olustur(..)) "
            + "|| execution(* com.raspel.erp.controller..*.*Olustur(..)) "
            + "|| execution(* com.raspel.erp.controller..*.guncelle(..)) "
            + "|| execution(* com.raspel.erp.controller..*.*Guncelle(..)) "
            + "|| execution(* com.raspel.erp.controller..*.durumGuncelle(..)) "
            + "|| execution(* com.raspel.erp.controller..*.*DurumGuncelle(..)) "
            + "|| execution(* com.raspel.erp.controller..*.sil(..)) "
            + "|| execution(* com.raspel.erp.controller..*.*Sil(..)) "
            + "|| execution(* com.raspel.erp.controller..*.*kes(..)) "
            + "|| execution(* com.raspel.erp.controller..*.*iptal(..)) "
            + "|| execution(* com.raspel.erp.controller..*.*GeriAl(..)) "
            + "|| execution(* com.raspel.erp.controller..*.*Aktar(..)) "
            + "|| execution(* com.raspel.erp.controller..*.*ode(..)) "
            + "|| execution(* com.raspel.erp.controller..*.*tamamla(..)) "
            + "|| execution(* com.raspel.erp.controller..*.*eslestir(..)) "
            + "|| execution(* com.raspel.erp.controller..*.*Kilit(..)) "
            // --- REDTEAM (Wave 2.2): asagidaki kalemler stok miktari, kasa,
            // donem kilidi veya bordro/onay durumu degistiriyordu ama denetim
            // izi birakmiyordu. Depo giris/cikar/transfer, stok duzeltme,
            // POS gun sonu, acilis devri, mali donem kilidi, bordro hesabi/
            // onayi, masraf talebi ve depo transferi onayi bu gruptadir.
            // Stok sayiminda "bu hareketi kim yapti?" sorusunun cevabi
            // bulunamazsa sayim tartismasi cozulemez.
            + "|| execution(* com.raspel.erp.controller..*.*Ekle(..)) "
            + "|| execution(* com.raspel.erp.controller..*.*Cikar(..)) "
            + "|| execution(* com.raspel.erp.controller..*.*Transfer(..)) "
            + "|| execution(* com.raspel.erp.controller..*.*Onayla(..)) "
            + "|| execution(* com.raspel.erp.controller..*.*onayla(..)) "
            + "|| execution(* com.raspel.erp.controller..*.*OnayKaldir(..)) "
            + "|| execution(* com.raspel.erp.controller..*.*onayKaldir(..)) "
            + "|| execution(* com.raspel.erp.controller..*.*Reddet(..)) "
            + "|| execution(* com.raspel.erp.controller..*.*reddet(..)) "
            + "|| execution(* com.raspel.erp.controller..*.*kilitle(..)) "
            + "|| execution(* com.raspel.erp.controller..*.*kilidiAc(..)) "
            + "|| execution(* com.raspel.erp.controller..*.*Kilitle(..)) "
            + "|| execution(* com.raspel.erp.controller..*.*KilidiAc(..)) "
            + "|| execution(* com.raspel.erp.controller..*.*Kapat(..)) "
            + "|| execution(* com.raspel.erp.controller..*.*kapat(..)) "
            + "|| execution(* com.raspel.erp.controller..*.*aktifYap(..)) "
            + "|| execution(* com.raspel.erp.controller..*.*duzelt(..)) "
            + "|| execution(* com.raspel.erp.controller..*.tara(..)) "
            + "|| execution(* com.raspel.erp.controller..*.*Tara(..)) "
            + "|| execution(* com.raspel.erp.controller..*.*hesapla(..)) "
            + "|| execution(* com.raspel.erp.controller..*.*Hesapla(..)) "
            + "|| execution(* com.raspel.erp.controller..*.*topluUret(..)) "
            + "|| execution(* com.raspel.erp.controller..*.*TopluUret(..)) "
            + "|| execution(* com.raspel.erp.controller..*.*gunSonu(..)) "
            + "|| execution(* com.raspel.erp.controller..*.*GunSonu(..)) "
            + "|| execution(* com.raspel.erp.controller..*.*acilis(..)) "
            + "|| execution(* com.raspel.erp.controller..*.*Acilis(..)) "
            + "|| execution(* com.raspel.erp.controller..*.aktar(..)) "
            + "|| execution(* com.raspel.erp.controller..*.*aktarimYap(..)) "
            + "|| execution(* com.raspel.erp.controller..*.*Donustur(..)) "
            + "|| execution(* com.raspel.erp.controller..*.*Cevir(..)) "
            + "|| execution(* com.raspel.erp.controller..*.*gonder(..)) "
            + "|| execution(* com.raspel.erp.controller..*.*Gonder(..)) "
            + "|| execution(* com.raspel.erp.controller..*.*Dosya(..)) "
            + "|| execution(* com.raspel.erp.controller..*.*Test(..)) "
            // Wave 4 (sifre kasasi): arsivleme fiziksel silme DEGIL, ama
            // kaydin gorunurluk/kullanim durumunu degistirir. "kim arsivledi,
            // kim geri aldi" sorusunun cevabi denetim izinde bulunmalidir.
            + "|| execution(* com.raspel.erp.controller..*.*arsivle(..)) "
            + "|| execution(* com.raspel.erp.controller..*.*Arsivle(..))")
    public void kritikPointcut() {}

    @AfterReturning("kritikPointcut()")
    public void logIslem(JoinPoint jp) {
        log(jp, islemEtiketi(jp));
    }

    @AfterThrowing(pointcut = "kritikPointcut()", throwing = "ex")
    public void logError(JoinPoint jp, Throwable ex) {
        log(jp, "HATA");
    }

    /** Metot adindan denetim islem etiketi turetir (OLUSTUR/GUNCELLE/SIL/ISLEM). */
    private String islemEtiketi(JoinPoint jp) {
        String ad = jp.getSignature().getName();
        if ("olustur".equals(ad) || ad.endsWith("Olustur")) return "OLUSTUR";
        if ("guncelle".equals(ad) || ad.endsWith("Guncelle")) return "GUNCELLE";
        if ("sil".equals(ad) || ad.endsWith("Sil")) return "SIL";
        // Arsivleme (soft delete) gercek silme DEGILDIR; ayri etiketlenir ki
        // denetim ekraninda "SIL" ile karismasin.
        if ("arsivle".equals(ad) || ad.endsWith("Arsivle")) return "ARSIVLE";
        if (ad.endsWith("ArsivleGeriAl") || ad.endsWith("arsivleGeriAl")) return "ARSIVLE_GERI_AL";
        return "ISLEM";
    }

    private void log(JoinPoint jp, String islem) {
        try {
            HttpServletRequest req = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
            Long kullaniciId = (Long) req.getAttribute("kullaniciId");
            Long sirketId = (Long) req.getAttribute("sirketId");
            String ip = com.raspel.erp.util.IstekYardimci.istemciIp(req);
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
            return hassasAlanlariGizle(objectMapper.writeValueAsString(hedef));
        } catch (Exception e) {
            return null;
        }
    }

    /** JSON'daki hassas alanları (şifre, token vb.) "***" ile maskeler. */
    private String hassasAlanlariGizle(String json) {
        try {
            JsonNode node = objectMapper.readTree(json);
            gizle(node);
            return objectMapper.writeValueAsString(node);
        } catch (Exception e) {
            return json;
        }
    }

    private void gizle(JsonNode node) {
        if (node == null) return;
        if (node.isObject()) {
            List<String> keys = new ArrayList<>();
            node.fieldNames().forEachRemaining(keys::add);
            for (String key : keys) {
                if (HASSAS_ALANLAR.contains(key.toLowerCase())) {
                    ((ObjectNode) node).put(key, "***");
                } else {
                    gizle(node.get(key));
                }
            }
        } else if (node.isArray()) {
            for (JsonNode child : node) {
                gizle(child);
            }
        }
    }
}
