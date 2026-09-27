package com.raspel.erp.controller.sistem;

import com.raspel.erp.config.TenantChecker;
import com.raspel.erp.exception.ResourceNotFoundException;
import com.raspel.erp.repository.envanter.StokRepository;
import com.raspel.erp.service.sistem.PazaryeriService;
import com.raspel.erp.service.sistem.SmsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * SMS ve pazaryeri entegrasyon uç noktaları. Sağlayıcı bilgileri yapılandırılmadıysa
 * açık hata döner (sahte başarı üretilmez); canlı gönderim müşteri kimliğiyle açılır.
 */
@Tag(name = "Entegrasyonlar", description = "SMS ve pazaryeri adaptör uç noktaları")
@RestController
@RequestMapping("/api/entegrasyon")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class EntegrasyonController {

    private final SmsService smsService;
    private final PazaryeriService pazaryeriService;
    private final StokRepository stokRepository;
    private final TenantChecker tenantChecker;

    @GetMapping("/durum")
    @Operation(summary = "Entegrasyon durumu", description = "SMS ve pazaryeri sağlayıcılarının yapılandırılıp yapılandırılmadığını döner")
    public ResponseEntity<Map<String, Object>> durum() {
        Map<String, Object> sonuc = new LinkedHashMap<>();
        sonuc.put("smsTanimli", smsService.saglayiciTanimliMi());
        sonuc.put("pazaryeriTanimli", pazaryeriService.saglayiciTanimliMi());
        return ResponseEntity.ok(sonuc);
    }

    @PostMapping("/sms")
    @Operation(summary = "SMS gönder", description = "Yapılandırılmış SMS sağlayıcısı üzerinden mesaj gönderir")
    public ResponseEntity<Map<String, Object>> smsGonder(@RequestBody Map<String, String> istek) {
        smsService.gonder(istek.get("telefon"), istek.get("mesaj"));
        return ResponseEntity.ok(Map.of("durum", "GONDERILDI"));
    }

    @PostMapping("/pazaryeri/stok/{stokId}")
    @Operation(summary = "Pazaryerine stok gönder", description = "Ürünün güncel miktar ve satış fiyatını pazaryerine bildirir")
    public ResponseEntity<Map<String, Object>> pazaryeriStokGonder(@PathVariable Long stokId,
                                                                    HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        var stok = stokRepository.findById(stokId)
                .orElseThrow(() -> new ResourceNotFoundException("Stok", stokId));
        tenantChecker.check(stok.getSirketId(), "Stok");
        pazaryeriService.stokGonder(stok.getId(), stok.getStokKodu(),
                stok.getMiktar(), stok.getSatisFiyati() != null ? stok.getSatisFiyati() : stok.getFiyat());
        return ResponseEntity.ok(Map.of("durum", "GONDERILDI", "sirketId", sirketId != null ? sirketId : 0L));
    }
}
