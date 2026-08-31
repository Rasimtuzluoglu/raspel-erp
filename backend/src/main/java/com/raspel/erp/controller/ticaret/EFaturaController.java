package com.raspel.erp.controller.ticaret;

import com.raspel.erp.dto.ticaret.EFaturaDTO;
import com.raspel.erp.service.ticaret.EFaturaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import com.raspel.erp.entity.ticaret.Fatura;

@Tag(name = "E-Fatura", description = "E-Fatura & E-İrsaliye entegrasyon API")
@RestController
@RequestMapping("/api/e-fatura")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'USER')")
public class EFaturaController {

    private final EFaturaService eFaturaService;

    @GetMapping
    @Operation(summary = "Tüm E-Faturaları getir", description = "Şirkete ait E-Faturaları listeler")
    public ResponseEntity<Page<EFaturaDTO>> tumu(HttpServletRequest request, @PageableDefault(size = 50) Pageable pageable) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        return ResponseEntity.ok(eFaturaService.eFaturalariGetir(sirketId, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "ID'ye göre E-Fatura getir", description = "E-Fatura detayını ve durumunu getirir")
    public ResponseEntity<EFaturaDTO> getir(@PathVariable Long id) {
        return ResponseEntity.ok(eFaturaService.eFaturaGetir(id));
    }

    @PostMapping("/olustur/{faturaId}")
    @Operation(summary = "E-Fatura taslağı oluştur", description = "Fatura için UBL-TR 2.1 E-Fatura taslağı oluşturur")
    public ResponseEntity<EFaturaDTO> olustur(
            @PathVariable Long faturaId,
            @RequestParam(required = false, defaultValue = "TEMELFATURA") String senaryo,
            @RequestParam(required = false, defaultValue = "SATIS") String tip,
            HttpServletRequest request) {
        Long sirketId = (Long) request.getAttribute("sirketId");
        EFaturaDTO dto = eFaturaService.eFaturaOlustur(faturaId, senaryo, tip, sirketId);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @PostMapping("/{id}/gib-gonder")
    @Operation(summary = "E-Faturayı GİB'e gönder", description = "Hazırlanan E-Faturayı GİB entegratörüne iletir")
    public ResponseEntity<EFaturaDTO> gibGonder(@PathVariable Long id) {
        return ResponseEntity.ok(eFaturaService.gibGonder(id));
    }

    @PostMapping("/{id}/durum-sorgula")
    @Operation(summary = "GİB durumunu sorgula", description = "GİB/entegratörden güncel durum kodunu sorgular ve kaydı günceller")
    public ResponseEntity<EFaturaDTO> durumSorgula(@PathVariable Long id) {
        return ResponseEntity.ok(eFaturaService.durumSorgula(id));
    }

    @GetMapping("/{id}/xml")
    @Operation(summary = "UBL-TR XML İndir", description = "E-Faturaya ait UBL-TR 2.1 XML belgesini indirir")
    public ResponseEntity<byte[]> xmlIndir(@PathVariable Long id) {
        String xml = eFaturaService.xmlIndir(id);
        byte[] bytes = xml.getBytes(StandardCharsets.UTF_8);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_XML);
        headers.setContentDisposition(org.springframework.http.ContentDisposition.attachment().filename("e-fatura-" + id + ".xml").build());
        return ResponseEntity.ok().headers(headers).body(bytes);
    }
}