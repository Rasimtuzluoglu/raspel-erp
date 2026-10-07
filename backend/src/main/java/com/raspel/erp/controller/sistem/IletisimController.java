package com.raspel.erp.controller.sistem;

import com.raspel.erp.config.TenantChecker;
import com.raspel.erp.entity.finans.CariHesap;
import com.raspel.erp.exception.BusinessException;
import com.raspel.erp.exception.ResourceNotFoundException;
import com.raspel.erp.repository.finans.CariHesapRepository;
import com.raspel.erp.service.sistem.EmailPolitikaService;
import com.raspel.erp.service.sistem.EmailService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.HtmlUtils;

import java.util.Map;

/**
 * Faz 3.5: Mail &amp; WhatsApp iletişim merkezi. Cariye manuel e-posta gönderimi
 * ve WhatsApp (wa.me) bağlantısı üretir.
 */
@Tag(name = "İletişim", description = "Mail & WhatsApp iletişim API")
@RestController
@RequestMapping("/api/iletisim")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasAnyRole('ADMIN', 'USER', 'MUHASEBE')")
public class IletisimController {

    private final CariHesapRepository cariHesapRepository;
    private final TenantChecker tenantChecker;
    private final EmailService emailService;
    private final EmailPolitikaService emailPolitikaService;

    public record MailIstek(
            @NotNull(message = "Cari seçilmelidir") Long cariHesapId,
            String konu,
            @NotBlank(message = "Mesaj boş olamaz") String mesaj) {}

    @PostMapping("/mail")
    @Operation(summary = "Cariye e-posta gönder",
            description = "Carinin kayıtlı e-posta adresine manuel e-posta gönderir (yalnızca kayıtlı alıcılar)")
    public ResponseEntity<Map<String, Object>> mailGonder(
            @RequestBody @Valid MailIstek istek, HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        CariHesap cari = cariHesapRepository.findById(istek.cariHesapId())
                .orElseThrow(() -> new ResourceNotFoundException("Cari Hesap", istek.cariHesapId()));
        tenantChecker.check(cari.getSirketId(), "Cari Hesap");
        if (cari.getEmail() == null || cari.getEmail().isBlank()) {
            throw new BusinessException("Cari için e-posta adresi tanımlı değil");
        }
        // Serbest alıcıya relay engeli: yalnızca şirketin kayıtlı e-postaları.
        emailPolitikaService.aliciDogrula(cari.getEmail(), sirketId);
        String konu = istek.konu() != null && !istek.konu().isBlank()
                ? istek.konu()
                : "RasPel ERP - Bilgilendirme";
        String html = "<p>" + HtmlUtils.htmlEscape(istek.mesaj()).replace("\n", "<br>") + "</p>";
        boolean gonderildi = emailService.htmlGonder(cari.getEmail(), konu, html);
        if (!gonderildi) {
            throw new BusinessException("E-posta gönderilemedi (SMTP yapılandırılmamış veya gönderim hatası)");
        }
        return ResponseEntity.ok(Map.of("durum", "GONDERILDI"));
    }

    @GetMapping("/whatsapp/{cariId}")
    @Operation(summary = "WhatsApp bağlantısı", description = "Cari telefonu için wa.me bağlantısı üretir")
    public ResponseEntity<Map<String, Object>> whatsapp(@PathVariable Long cariId) {
        CariHesap cari = cariHesapRepository.findById(cariId)
                .orElseThrow(() -> new ResourceNotFoundException("Cari Hesap", cariId));
        tenantChecker.check(cari.getSirketId(), "Cari Hesap");
        String tel = cari.getTelefon() != null && !cari.getTelefon().isBlank()
                ? cari.getTelefon() : cari.getYetkiliTelefon();
        if (tel == null || tel.isBlank()) {
            throw new BusinessException("Cari için telefon numarası tanımlı değil");
        }
        String rakamlar = tel.replaceAll("[^0-9]", "");
        if (rakamlar.startsWith("0")) {
            rakamlar = "90" + rakamlar.substring(1);
        } else if (rakamlar.length() == 10) {
            rakamlar = "90" + rakamlar;
        }
        return ResponseEntity.ok(Map.of(
                "telefon", tel,
                "link", "https://wa.me/" + rakamlar));
    }
}
